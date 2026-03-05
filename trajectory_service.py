from flask import Flask, request, jsonify
import mdtraj as md
import numpy as np
import os

app = Flask(__name__)

@app.route('/initialize', methods=['POST'])
def initialize():

    data = request.json
    trajectoryPath = data['trajectoryPath']
    struc_path = data['struc_path']
    print("In initialize")
    filesize = os.path.getsize(trajectoryPath)
    if filesize <= 1500000000:
        frame = md.load(trajectoryPath, top=struc_path)
        num_frames = frame.n_frames
        num_atoms = frame.n_atoms
    else:
        first_frame = md.load_frame(trajectoryPath, 0, struc_path)
        times = 0
        for chunk in md.iterload(trajectoryPath, top=struc_path, chunk=100, atom_indices=[1]):
            times += chunk.n_frames
        num_frames = times
        num_atoms = first_frame.n_atoms

    response = {
        'num_frames': num_frames,
        'num_atoms': num_atoms
    }

    return jsonify(response)

@app.route('/fetch_chunk', methods=['POST'])
def fetch_chunk():
    data = request.json

    trajectoryPath = data['trajectoryPath']
    struc_path = data['struc_path']
    chunk_size = data['chunk_size']
    start_frame = data['start_frame']
    
    print("In fetch**********************************")
    print("trajectoryPath:: ",trajectoryPath)
    print("struc_path:: ",struc_path)
    print("chunk_size:: ",chunk_size)
    print("start_frame:: ",start_frame)

    # Load the frames in the specified range
    frames = []
    for i in range(start_frame, start_frame + chunk_size):
        try:
            frame = md.load_frame(trajectoryPath, i, top=struc_path)
            frames.append(frame)
        except IndexError:
            # If we go out of bounds, break the loop
            print(f"IndexError: Frame {i} out of bounds")
            break
        except OverflowError as oe:
            print(f"OverflowError: {oe}")
            return jsonify({'error': str(oe)}), 500
        except KeyError as ke:
            print(f"KeyError: {ke}")
            return jsonify({'error': str(ke)}), 500
        except Exception as e:
            print(f"Exception: {e}")
            return jsonify({'error': str(e)}), 500



    # Concatenate frames into a single trajectory object
    if frames:
        chunk = md.join(frames)
        positions = chunk.xyz.tolist()  # Convert positions to a serializable format
        print("Position::", positions)
        
        # Retrieve box data if available
        if chunk.unitcell_vectors is not None:
            boxes = chunk.unitcell_vectors.tolist()
        else:
            boxes = None
    else:
        positions=[]
        boxes=[]


    response = {
        'struc_path':struc_path,
        'traj_path':trajectoryPath,
        'positions': positions,
        'boxes': boxes
    }

    return jsonify(response)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')

