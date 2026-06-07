package com.sante.lims.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Result {

    public enum ResultFormat { NUMERIC, TEXT, PDF, IMAGE }

    private UUID          id;
    private UUID          requestId;
    private ResultFormat  resultFormat;
    private BigDecimal    numericValue;
    private String        textValue;
    private String        filePath;
    private boolean       validated;
    private UUID          validatedBy;
    private LocalDateTime validatedAt;
    private boolean       notificationSent;
    private UUID          uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;

    public Result() {}

    // Getters 
    public UUID          getId()               { return id; }
    public UUID          getRequestId()        { return requestId; }
    public ResultFormat  getResultFormat()     { return resultFormat; }
    public BigDecimal    getNumericValue()     { return numericValue; }
    public String        getTextValue()        { return textValue; }
    public String        getFilePath()         { return filePath; }
    public boolean       isValidated()         { return validated; }
    public UUID          getValidatedBy()      { return validatedBy; }
    public LocalDateTime getValidatedAt()      { return validatedAt; }
    public boolean       isNotificationSent()  { return notificationSent; }
    public UUID          getUploadedBy()       { return uploadedBy; }
    public LocalDateTime getUploadedAt()       { return uploadedAt; }
    public LocalDateTime getUpdatedAt()        { return updatedAt; }

    // Setters
    public void setId(UUID id)                          { this.id = id; }
    public void setRequestId(UUID requestId)            { this.requestId = requestId; }
    public void setResultFormat(ResultFormat rf)        { this.resultFormat = rf; }
    public void setNumericValue(BigDecimal v)           { this.numericValue = v; }
    public void setTextValue(String v)                  { this.textValue = v; }
    public void setFilePath(String filePath)            { this.filePath = filePath; }
    public void setValidated(boolean validated)         { this.validated = validated; }
    public void setValidatedBy(UUID id)                 { this.validatedBy = id; }
    public void setValidatedAt(LocalDateTime t)         { this.validatedAt = t; }
    public void setNotificationSent(boolean b)          { this.notificationSent = b; }
    public void setUploadedBy(UUID id)                  { this.uploadedBy = id; }
    public void setUploadedAt(LocalDateTime t)          { this.uploadedAt = t; }
    public void setUpdatedAt(LocalDateTime t)           { this.updatedAt = t; }
}
