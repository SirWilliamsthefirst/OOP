package com.sante.lims.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TestType {

    public enum ResultFormat { NUMERIC, TEXT, PDF, IMAGE }

    private UUID          id;
    private String        name;
    private String        category;
    private BigDecimal    price;
    private int           tatHours;        // Standard Turnaround Time in hours
    private ResultFormat  resultFormat;
    private String        description;
    private boolean       active;
    private UUID          createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TestType() {}

    // ── Getters ─────────────────────────────────────────────────
    public UUID         getId()           { return id; }
    public String       getName()         { return name; }
    public String       getCategory()     { return category; }
    public BigDecimal   getPrice()        { return price; }
    public int          getTatHours()     { return tatHours; }
    public ResultFormat getResultFormat() { return resultFormat; }
    public String       getDescription()  { return description; }
    public boolean      isActive()        { return active; }
    public UUID         getCreatedBy()    { return createdBy; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getUpdatedAt()   { return updatedAt; }

    // ── Setters ─────────────────────────────────────────────────
    public void setId(UUID id)                        { this.id = id; }
    public void setName(String name)                  { this.name = name; }
    public void setCategory(String category)          { this.category = category; }
    public void setPrice(BigDecimal price)            { this.price = price; }
    public void setTatHours(int tatHours)             { this.tatHours = tatHours; }
    public void setResultFormat(ResultFormat rf)      { this.resultFormat = rf; }
    public void setDescription(String description)    { this.description = description; }
    public void setActive(boolean active)             { this.active = active; }
    public void setCreatedBy(UUID createdBy)          { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime t)         { this.createdAt = t; }
    public void setUpdatedAt(LocalDateTime t)         { this.updatedAt = t; }
}
