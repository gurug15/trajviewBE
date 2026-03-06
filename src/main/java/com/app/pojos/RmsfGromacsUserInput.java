package com.app.pojos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RmsfGromacsUserInput {
	@NotBlank(message = "topology filename can't be blank")
	private String topologyFileName;
	@NotBlank(message = "trajectory fileName can't be blank")
	private String trajectoryFileName;
	@NotBlank(message = "output fileName can't be blank")
	private String outputfileName;
//	@NotBlank(message = "Index fileName can't be blank")
	private String indexFileName;
	private int grouplsfit;
	@Min(value = 1, message = "first Frame number must have > 1")
	private int firstFrameno;
	private int lastFrameNo;
	private boolean superimpose;
	private boolean residueRMSF;
	private String logFileName;

}
