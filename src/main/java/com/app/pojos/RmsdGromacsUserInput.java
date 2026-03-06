package com.app.pojos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RmsdGromacsUserInput {
	@NotBlank(message = "topology filename can't be blank")
	private String topologyFileName;
	@NotBlank(message = "trajectory fileName can't be blank")
	private String trajectoryFileName;
//	@NotBlank(message = "Index fileName can't be blank")
	private String indexFileName;
	@NotBlank(message = "output fileName can't be blank")
	private String outputfileName;
	private int grouplsfit;
	private int groupRMSD;
//	@Min(value = 0, message = "first Frame number must have > 1")
	private int firstFrameno;
	private int lastFrameNo;
}
