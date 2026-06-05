package com.sante.lims.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class TestRequest {

    public enum PaymentStatus { UNPAID, PAID }
    public enum Status { PENDING, SAMPLE_COLLECTED, PROCESSING, VALIDATING, COMPLETED, CANCELLED }

    private UUID          id;
    private UUID          customerId;
    private String        customerName;   // joined field – convenience
    private UUID          testTypeId;
    private String        testTypeName;   // joined field – convenience
    private PaymentStatus paymentStatus;
    private UUID          paymentMarkedBy;
    private LocalDateTime paymentMarkedAt;
    private Status        status;
    private LocalDateTime resultReadyAt;  // drives customer countdown timer
    private String        notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TestRequest() {}

    // ── Getters ─────────────────────────────────────────────────
    public UUID          getId()              { return id; }
    public UUID          getCustomerId()      { return customerId; }
    public String        getCustomerName()    { return customerName; }
    public UUID          getTestTypeId()      { return testTypeId; }
    public String        getTestTypeName()    { return testTypeName; }
    public PaymentStatus getPaymentStatus()   { return paymentStatus; }
    public UUID          getPaymentMarkedBy() { return paymentMarkedBy; }
    public LocalDateTime getPaymentMarkedAt() { return paymentMarkedAt; }
    public Status        getStatus()          { return status; }
    public LocalDateTime getResultReadyAt()   { return resultReadyAt; }
    public String        getNotes()           { return notes; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }

    // ── Setters ─────────────────────────────────────────────────
    public void setId(UUID id)                           { this.id = id; }
    public void setCustomerId(UUID customerId)           { this.customerId = customerId; }
    public void setCustomerName(String n)                { this.customerName = n; }
    public void setTestTypeId(UUID testTypeId)           { this.testTypeId = testTypeId; }
    public void setTestTypeName(String n)                { this.testTypeName = n; }
    public void setPaymentStatus(PaymentStatus ps)       { this.paymentStatus = ps; }
    public void setPaymentMarkedBy(UUID id)              { this.paymentMarkedBy = id; }
    public void setPaymentMarkedAt(LocalDateTime t)      { this.paymentMarkedAt = t; }
    public void setStatus(Status status)                 { this.status = status; }
    public void setResultReadyAt(LocalDateTime t)        { this.resultReadyAt = t; }
    public void setNotes(String notes)                   { this.notes = notes; }
    public void setCreatedAt(LocalDateTime t)            { this.createdAt = t; }
    public void setUpdatedAt(LocalDateTime t)            { this.updatedAt = t; }
}
