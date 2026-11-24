
package com.garmentqc.system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "aql_sampling_plans", uniqueConstraints = {
        @UniqueConstraint(name = "uk_aql_sampling_code_level", columnNames = { "lot_size_code_id", "aql_level_id" })
})
public class AqlSamplingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_size_code_id", nullable = false)
    private AqlLotSizeCode lotSizeCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aql_level_id", nullable = false)
    private AqlLevel aqlLevel;

    @Column(name = "effective_code_letter", length = 2)
    private String effectiveCodeLetter;

    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    @Column(name = "acceptance_number")
    private Integer acceptanceNumber;

    @Column(name = "rejection_number")
    private Integer rejectionNumber;

    public Long getId() {
        return id;
    }

    public AqlLotSizeCode getLotSizeCode() {
        return lotSizeCode;
    }

    public void setLotSizeCode(AqlLotSizeCode lotSizeCode) {
        this.lotSizeCode = lotSizeCode;
    }

    public AqlLevel getAqlLevel() {
        return aqlLevel;
    }

    public void setAqlLevel(AqlLevel aqlLevel) {
        this.aqlLevel = aqlLevel;
    }

    public String getEffectiveCodeLetter() {
        return effectiveCodeLetter;
    }

    public void setEffectiveCodeLetter(String effectiveCodeLetter) {
        this.effectiveCodeLetter = effectiveCodeLetter;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Integer getAcceptanceNumber() {
        return acceptanceNumber;
    }

    public void setAcceptanceNumber(Integer acceptanceNumber) {
        this.acceptanceNumber = acceptanceNumber;
    }

    public Integer getRejectionNumber() {
        return rejectionNumber;
    }

    public void setRejectionNumber(Integer rejectionNumber) {
        this.rejectionNumber = rejectionNumber;
    }

}
