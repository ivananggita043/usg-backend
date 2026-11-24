
package com.garmentqc.system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "aql_lot_size_codes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_aql_lot_size_range", columnNames = { "min_lot_size", "max_lot_size" })
})
public class AqlLotSizeCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_lot_size", nullable = false)
    private Integer minLotSize;

    @Column(name = "max_lot_size")
    private Integer maxLotSize;

    @Column(name = "inspection_level", nullable = false)
    private String inspectionLevel = "II";

    @Column(name = "code_letter", nullable = false, length = 2)
    private String codeLetter;

    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    public Long getId() {
        return id;
    }

    public Integer getMinLotSize() {
        return minLotSize;
    }

    public void setMinLotSize(Integer minLotSize) {
        this.minLotSize = minLotSize;
    }

    public Integer getMaxLotSize() {
        return maxLotSize;
    }

    public void setMaxLotSize(Integer maxLotSize) {
        this.maxLotSize = maxLotSize;
    }

    public String getInspectionLevel() {
        return inspectionLevel;
    }

    public void setInspectionLevel(String inspectionLevel) {
        this.inspectionLevel = inspectionLevel;
    }

    public String getCodeLetter() {
        return codeLetter;
    }

    public void setCodeLetter(String codeLetter) {
        this.codeLetter = codeLetter;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }
}
