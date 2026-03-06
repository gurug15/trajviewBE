package com.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrajectoryFrameInput {
    @NotBlank(message = "topology filename can't be blank")
    private String topologyFileName;
    @NotBlank(message = "trajectory fileName can't be blank")
    private String trajectoryFileName;
    @Min(value = 0, message = "frame number must be >= 0")
    private int picoSecNumber;
//    @NotBlank(message = "userName can't be blank")
    private String userName;
}
