package org.simpmc.simppay.handler.banking.data;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Data
@Builder
public class BankingData {
    public UUID playerUUID;
    public String desc;
    public String bin;
    public double amount;
    public String accountNumber;
    public String url;

    @Nullable
    public String bankName;

    @Nullable
    public String qrString;

    @Nullable
    public String qrImageUrl;
}
