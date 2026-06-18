from flask import Flask, request, jsonify, Response
from aihandler import ImageClassificationPipeline, createClassifier, predictAnimal
from PIL import Image
import io

type APIReturn = tuple[Response, int | None]
ALLOWED_EXTENSIONS: set[str] = {'png', 'jpg', 'jpeg'}

app = Flask(__name__)
classifier: ImageClassificationPipeline = createClassifier()

@app.route("/")
def homepage() -> APIReturn:
    return jsonify({
        "success": True
    })

@app.route("/identify", methods=["POST"])
def identifyAnimal() -> APIReturn:
    if 'image' not in request.files:
        return jsonify({
            "success": False
        }), 400
    
    file = request.files['image']
    if file.filename == '' or not isFileAllowed(file.filename):
        return jsonify({
            "success": False
        }), 400
    
    # TODO: Identify Animal
    image: Image = Image.open(io.BytesIO(file.read())).convert("RGB")
    prediction: dict[str, float] = predictAnimal(classifier, image)
    animalName: str = max(prediction, key=prediction.get)

    return jsonify({
        "success": True,
        "animalName": animalName.lower()
    })

def isFileAllowed(filename: str) -> bool:
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000)
