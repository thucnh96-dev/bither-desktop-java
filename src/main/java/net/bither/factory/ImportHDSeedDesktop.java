/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.factory;

import net.bither.Bither;
import net.bither.bitherj.core.HDMKeychain;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.factory.ImportHDSeed;
import net.bither.utils.KeyUtil;
import net.bither.utils.LocaliserUtils;
import net.bither.viewsystem.dialogs.MessageDialog;

import javax.swing.*;
import java.util.List;

public class ImportHDSeedDesktop extends ImportHDSeed {


    public ImportHDSeedDesktop(String content, SecureCharSequence password) {
        super(ImportHDSeedType.HDMColdSeedQRCode, content, null, password);

    }

    public ImportHDSeedDesktop(List<String> worlds, SecureCharSequence password) {
        super(ImportHDSeedType.HDMColdPhrase, null, worlds, password);

    }


    public void importColdSeed() {
        new Thread() {
            @Override
            public void run() {
                HDMKeychain result = importHDSeed();
                if (result != null) {

                    KeyUtil.setHDKeyChain(result);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Bither.getCoreController().fireRecreateAllViews(true);
                            new MessageDialog(LocaliserUtils.getString("import_private_key_qr_code_success")).showMsg();
                        }
                    });
                    Bither.refreshFrame();

                }
            }
        }.start();

    }

    @Override
    public void importError(final int errorCode) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String meessage;
                switch (errorCode) {

                    case PASSWORD_IS_DIFFEREND_LOCAL:
                        meessage = LocaliserUtils.getString("import_private_key_qr_code_failed_different_password");
                        break;
                    case NOT_HDM_COLD_SEED:
                        meessage = LocaliserUtils.getString("import_hdm_cold_seed_format_error");
                        break;
                    default:
                        meessage = LocaliserUtils.getString("import_private_key_qr_code_failed");

                        break;
                }
                new MessageDialog(meessage).showMsg();

            }
        });

    }
}
