package org.simpmc.simppay.forms;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.ModalForm;
import org.simpmc.simppay.commands.sub.banking.CancelCommand;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.menu.FormsConfig;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.util.FloodgateUtil;
import org.simpmc.simppay.util.MessageUtil;

public class BankingForm {

    public static boolean send(Player player, BankingData bankingData) {
        if (bankingData == null) {
            MessageUtil.warn("[BankingForm] Cannot send Bedrock banking form: banking data is missing for " + player.getName());
            return false;
        }

        boolean sent = FloodgateUtil.sendForm(player, getBankingForm(player, bankingData));
        if (!sent) {
            MessageUtil.warn("[BankingForm] Failed to send Bedrock banking form to player: " + player.getName());
        }
        return sent;
    }

    public static ModalForm getBankingForm(Player player, BankingData bankingData) {
        FormsConfig.BankingFormStrings f = ConfigManager.getInstance().getConfig(FormsConfig.class).bankingForm;

        return ModalForm.builder()
                .title(f.title)
                .content(buildContent(bankingData, f))
                .button1(f.closeButtonLabel)
                .button2(f.cancelButtonLabel)
                .validResultHandler((form, result) -> {
                    if (result.clickedButtonId() == 1) {
                        CancelCommand.cancel(player);
                    }
                })
                .build();
    }

    public static String buildContent(BankingData bankingData, FormsConfig.BankingFormStrings f) {
        String bankName = bankingData.getBankName();
        if (bankName == null || bankName.isBlank()) {
            bankName = bankingData.getBin();
        }
        if (bankName == null || bankName.isBlank()) {
            bankName = f.unknownBankName;
        }

        return String.format(
                f.contentFormat,
                nullToEmpty(bankingData.getAccountNumber()),
                bankName,
                MessageUtil.formatVietnameseCurrency(bankingData.getAmount()),
                nullToEmpty(bankingData.getDesc())
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
