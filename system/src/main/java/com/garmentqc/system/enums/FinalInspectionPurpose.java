
package com.garmentqc.system.enums;

public enum FinalInspectionPurpose {

    PRE_FINAL_20_PERCENT("PRE FINAL 20 %"),
    PRE_FINAL_60_PERCENT("PRE FINAL 60 %"),
    PRE_FINAL_80_PERCENT("PRE FINAL 80 %"),
    RE_FINAL_20_PERCENT("RE FINAL 20 %"),
    RE_FINAL_60_PERCENT("RE FINAL 60 %"),
    RE_FINAL_80_PERCENT("RE FINAL 80 %"),
    FINAL_FACTORY("FINAL FACTORY");

    private final String label;

    FinalInspectionPurpose(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
