/*
 *
 *  Copyright 2014 http://Bither.net
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package net.bither.viewsystem.froms.desktop.hdm;

import net.bither.bitherj.core.*;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.qrcode.QRCodeTxTransport;
import net.bither.bitherj.utils.Utils;
import net.bither.qrcode.DesktopQRCodReceive;
import net.bither.qrcode.DesktopQRCodSend;
import net.bither.utils.FileUtil;
import net.bither.viewsystem.dialogs.AbstractDesktopHDMMsgDialog;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DesktopHDMMsgHotDialog extends AbstractDesktopHDMMsgDialog {


    private static final long CHECK_TX_INTERVAL = 3 * 1000;

    private Tx tx;

    private List<HashMap<String, Long>> addressAmtList = new ArrayList<HashMap<String, Long>>();
    private File addressAmtFile;
    private SecureCharSequence password;
    private DesktopHDMKeychain desktopHDMKeychain;

    public DesktopHDMMsgHotDialog(SecureCharSequence password) {
        isSendMode = true;
        this.password = password;
        desktopHDMKeychain = AddressManager.getInstance().getDesktopHDMKeychains().get(0);
    }

    @Override
    public void handleScanResult(final String result) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("scan :" + result);
                if (isSendMode) {
                    if (desktopQRCodSend != null) {
                        desktopQRCodSend.setReceiveMsg(result);
                    }

                    if (desktopQRCodSend.sendComplete()) {
                        showQRCode(desktopQRCodReceive.getShowMsg());

                    }
                    if (desktopQRCodSend.allComplete()) {
                        isSendMode = false;
                        desktopQRCodReceive = new DesktopQRCodReceive();
                    }
                } else {
                    desktopQRCodReceive.receiveMsg(result);
                    showQRCode(desktopQRCodReceive.getShowMsg());

                }

                if (desktopQRCodSend.allComplete() && desktopQRCodReceive.receiveComplete()) {
                    publishTx();

                }


            }
        });

    }

    public void publishTx() {
        String signStr = desktopQRCodReceive.getReceiveResult();

        if (addressAmtList.size() > 0) {
            addressAmtList.remove(0);
            saveFile(addressAmtList, addressAmtFile);
        }
        desktopQRCodReceive = null;
        desktopQRCodSend = null;


    }

    @Override
    protected void inited() {
        refreshTx();
    }

    private void refreshTx() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        if (desktopQRCodSend == null) {
                            getTx();
                        }
                        Thread.sleep(CHECK_TX_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }

    private void getTx() {
        try {

            if (desktopQRCodSend != null) {
                return;
            }
            if (addressAmtList.size() == 0) {
                addressAmtFile = getSendBitcoinFile();

                addressAmtList = getAddressAndAmts(addressAmtFile);
            }
            String address = null;
            long amt;
            if (addressAmtList.size() > 0) {
                for (HashMap<String, Long> hashMap : addressAmtList) {
                    for (Map.Entry<String, Long> kv : hashMap.entrySet()) {
                        address = kv.getKey();
                        amt = kv.getValue();
                        String changeAddress = desktopHDMKeychain.getNewChangeAddress();
                        tx = desktopHDMKeychain.newTx(address, amt);

                        List<DesktopHDMAddress> signingAddresses = desktopHDMKeychain.getSigningAddressesForInputs(tx.getIns());
                        List<AbstractHD.PathTypeIndex> pathTypeIndexList = new ArrayList<AbstractHD.PathTypeIndex>();
                        for (DesktopHDMAddress desktopHDMAddress : signingAddresses) {
                            AbstractHD.PathTypeIndex pathTypeIndex = new AbstractHD.PathTypeIndex();
                            pathTypeIndex.pathType = desktopHDMAddress.getPathType();
                            pathTypeIndex.index = desktopHDMAddress.getIndex();
                            pathTypeIndexList.add(pathTypeIndex);
                        }
                        desktopQRCodSend = new DesktopQRCodSend(tx, pathTypeIndexList, changeAddress);
                        showQRCode(desktopQRCodSend.getShowMessage());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void saveFile(List<HashMap<String, Long>> list, File file) {
        try {
            String result = "";
            for (HashMap<String, Long> hashMap : list) {
                for (Map.Entry<String, Long> kv : hashMap.entrySet()) {
                    result = result + kv.getKey() + "," + Long.toString(kv.getValue()) + "\n";
                }
            }

            Utils.writeFile(result.getBytes(), file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<HashMap<String, Long>> getAddressAndAmts(File file) {
        if (file != null) {
            String content = Utils.readFile(file);
            if (Utils.isEmpty(content)) {
                if (file.exists()) {
                    file.delete();
                }
            }
            String[] addrssAndAmts = content.split("\n");
            if (addrssAndAmts.length == 0) {
                if (file.exists()) {
                    file.delete();
                }
            }
            for (String str : addrssAndAmts) {
                String[] temp = str.split(",");
                if (temp.length > 1) {
                    if (Utils.validBicoinAddress(temp[0])) {
                        HashMap<String, Long> hashMap = new HashMap<String, Long>();
                        hashMap.put(temp[0], Long.valueOf(temp[1]));
                        addressAmtList.add(hashMap);
                    }
                }
            }
            if (addressAmtList.size() == 0) {
                if (file.exists()) {
                    file.delete();
                }
            }

        }
        return addressAmtList;


    }

    private File getSendBitcoinFile() {
        File file = FileUtil.getSendBitcoinDir();
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            return files[0];
        } else {
            return null;
        }
    }

}