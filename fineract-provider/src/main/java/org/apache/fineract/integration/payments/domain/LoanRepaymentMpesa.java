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
package org.apache.fineract.integration.payments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "m_loan_repayment_mpesa")
public class LoanRepaymentMpesa extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "client_id", nullable = true)
    private Long clientId;

    @Column(name = "loan_id", nullable = true)
    private Long loanId;

    @Column(name = "loan_transaction_id", nullable = true)
    private Long loanTransactionId;

    @Column(name = "transaction_type", nullable = false, length = 40)
    private String transactionType;

    @Column(name = "transaction_id", nullable = false, length = 40)
    private String transactionId;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @Column(name = "transaction_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal transactionAmount;

    @Column(name = "business_short_code", nullable = false, length = 12)
    private String businessShortCode;

    @Column(name = "bill_ref_number", nullable = false, length = 12)
    private String billRefNumber;

    @Column(name = "invoice_number", nullable = true, length = 100)
    private String invoiceNumber;

    @Column(name = "org_account_balance", scale = 6, precision = 19, nullable = true)
    private BigDecimal orgAccountBalance;

    @Column(name = "third_party_trans_id", nullable = true, length = 20)
    private String thirdPartyTransID;

    @Column(name = "msisdn", nullable = true, length = 20)
    private String msisdn;

    @Column(name = "first_name", nullable = true, length = 50)
    private String firstName;

}
