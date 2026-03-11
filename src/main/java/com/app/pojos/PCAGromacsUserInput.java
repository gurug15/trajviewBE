package com.app.pojos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PCAGromacsUserInput {
	  private String trajectoryFile;      // MD.xtc
	    private String tprFile;             // MD.tpr
	    private String indexFile;           // index.ndx
	    private String outputFolder;        // output directory name
	    private Integer stepSize;           // optional: time window (default 10)
	    private Integer maxTime; 
}
