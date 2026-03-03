package net.protsenko.refrigeration.engineer.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChamberData {

    private String name;

    private WallDimensions dimensions;
    private WallInsulation insulation;

    @JsonProperty("external_temperatures")
    private WallTemperatures externalTemperatures;

    private ProductInfo product;
    private VentilationInfo ventilation;
    private List<DoorInfo> doors;

    @JsonProperty("personnel_count")
    private Integer personnelCount;

    private LightingInfo lighting;

    @JsonProperty("loading_equipment")
    private List<EquipmentInfo> loadingEquipment;

    @JsonProperty("forklift_work_hours")
    private Double forkliftWorkHours;

    @JsonProperty("electrical_devices")
    private List<EquipmentInfo> electricalDevices;

    @JsonProperty("missing_parameters")
    private List<String> missingParameters;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WallDimensions {
        @JsonProperty("length_A") private Double lengthA;
        @JsonProperty("length_B") private Double lengthB;
        @JsonProperty("width_B")  private Double widthB;
        @JsonProperty("width_G")  private Double widthG;
        private Double height;
        private Double area;
        private Double volume;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WallInsulation {
        private InsulationLayer A;
        private InsulationLayer B;
        private InsulationLayer V;
        private InsulationLayer G;
        private InsulationLayer ceiling;
        private InsulationLayer floor;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InsulationLayer {
        private String material;
        @JsonProperty("thickness_mm") private Double thicknessMm;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WallTemperatures {
        private Double A;
        private Double B;
        private Double V;
        private Double G;
        private Double ceiling;
        private Double floor;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductInfo {
        @JsonProperty("product_type")           private String productType;
        @JsonProperty("incoming_temperature_c") private Double incomingTemperature;
        @JsonProperty("target_temperature_c")   private Double targetTemperature;
        @JsonProperty("chamber_temperature_c")  private Double chamberTemperature;
        @JsonProperty("chamber_humidity_pct")   private Double chamberHumidity;
        @JsonProperty("cooling_time_hours")     private Double coolingTimeHours;
        @JsonProperty("daily_turnover_kg")      private Double dailyTurnoverKg;
        @JsonProperty("mass_in_chamber_kg")     private Double massInChamberKg;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VentilationInfo {
        @JsonProperty("supply_rate")              private Double supplyRate;
        @JsonProperty("exhaust_rate")             private Double exhaustRate;
        @JsonProperty("supply_air_temperature_c") private Double supplyAirTemperature;
        @JsonProperty("supply_air_humidity_pct")  private Double supplyAirHumidity;
        @JsonProperty("recuperation_type")        private String recuperationType;
        @JsonProperty("additional_cooling")       private String additionalCooling;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DoorInfo {
        private String type;
        @JsonProperty("width_m")                private Double widthM;
        @JsonProperty("height_m")               private Double heightM;
        @JsonProperty("open_time_min_per_day")  private Double openTimeMinPerDay;
        @JsonProperty("outside_temperature_c")  private Double outsideTemperature;
        @JsonProperty("curtain_type")           private String curtainType;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LightingInfo {
        private Boolean present;
        private String type;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EquipmentInfo {
        private String type;
        private Integer quantity;
    }
}