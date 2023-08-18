/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.integration.payments.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.integration.payments.domain.ConfirmationPaymentData;
import org.apache.fineract.integration.payments.domain.ConfirmationPaymentRequest;
import org.apache.fineract.integration.payments.domain.LoanRepaymentMpesa;
import org.apache.fineract.integration.payments.domain.LoanRepaymentMpesaRepository;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionRepaymentData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentType;
import org.apache.fineract.portfolio.paymenttype.domain.PaymentTypeRepositoryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty("fineract.mpesa.enabled")
public class MPesaPaymentsService {

    private final ClientReadPlatformService clientReadPlatformService;
    private final LoanRepositoryWrapper loanRepository;
    private final LoanWritePlatformService loanWritePlatformService;
    private final PaymentTypeRepositoryWrapper paymentTypeRepository;
    private final LoanRepaymentMpesaRepository loanRepaymentMpesaRepository;
    private final FineractProperties fineractProperties;
    private final FromJsonHelper fromApiJsonHelper;
    private final Gson gson = GoogleGsonSerializerHelper.createSimpleGson();

    public ConfirmationPaymentData paymentConfirmation(ConfirmationPaymentRequest request) {

        // Store the Payment Request
        final LocalDateTime transactionTime = DateUtils.getLocalDateTimeOf(request.getTransTime(), DateUtils.SIMPLE_DATETIME_FORMATTER);
        LoanRepaymentMpesa mPesaPayment = new LoanRepaymentMpesa(null, null, null, request.getTransactionType(), request.getTransID(),
                transactionTime, request.getTransAmount(), request.getBusinessShortCode(), request.getBillRefNumber(),
                request.getInvoiceNumber(), request.getOrgAccountBalance(), request.getThirdPartyTransID(), request.getMSISDN(),
                request.getFirstName());

        // Apply the Loan Repayment Transaction
        final ExternalId clientExternalId = ExternalIdFactory.produce(request.getBillRefNumber());
        final Long clientId = clientReadPlatformService.retrieveClientIdByExternalId(clientExternalId);
        final ExternalId transactionExternalId = ExternalIdFactory.produce(request.getTransID());

        String txnId = ExternalId.generate().getValue();
        if (clientId != null) {
            log.debug("Client with externalId {} was found with Client Id {}", request.getBillRefNumber(), clientId);
            mPesaPayment.setClientId(clientId);

            final Long loanId = loanRepository.findActiveLoanIdByClienId(clientId);
            if (loanId != null) {
                log.debug("Client with externalId {} has as Active Loan Id {}", request.getBillRefNumber(), loanId);
                mPesaPayment.setLoanId(loanId);

                try {
                    log.debug("Payment type {}", fineractProperties.getMpesa().getPaymentType());
                    final PaymentType paymentType = paymentTypeRepository.findByName(fineractProperties.getMpesa().getPaymentType());
                    final boolean isRecoveryRepayment = false;
                    final String note = request.getTransactionType() + " " + request.getTransID();
                    final String transactionDate = transactionTime.format(DateUtils.DEFAULT_DATE_FORMATTER);
                    final LoanTransactionRepaymentData repaymentData = new LoanTransactionRepaymentData(transactionDate,
                            request.getTransAmount(), transactionExternalId, request.getMSISDN(), paymentType.getId(), note,
                            DateUtils.DEFAULT_DATE_FORMAT, Locale.US.getLanguage());
                    final String jsonData = gson.toJson(repaymentData);
                    log.info("Loan id {} Repayment {}", loanId, jsonData);
                    JsonElement parsedCommand = gson.fromJson(jsonData, JsonElement.class);
                    JsonCommand command = JsonCommand.from(jsonData, parsedCommand, fromApiJsonHelper, null, loanId, null, null, clientId,
                            loanId, null, null, null, null, null, null, null);

                    CommandProcessingResult commandResult = loanWritePlatformService.makeLoanRepayment(LoanTransactionType.REPAYMENT,
                            loanId, command, isRecoveryRepayment);

                    txnId = commandResult.getResourceId().toString();
                    mPesaPayment.setLoanTransactionId(commandResult.getResourceId());

                } catch (final JpaSystemException | DataIntegrityViolationException dve) {
                    log.error("{}", dve.getMostSpecificCause());
                }
            } else {
                log.error("Client {} : {} active loan was not found", clientId, request.getBillRefNumber());
            }
        } else {
            log.error("Client with externalId {} was not found for transaction {}", request.getBillRefNumber(), request.getTransID());
        }
        loanRepaymentMpesaRepository.saveAndFlush(mPesaPayment);
        return new ConfirmationPaymentData(txnId, "0", "success");

    }

}
