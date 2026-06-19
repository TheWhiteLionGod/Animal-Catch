from flask import Flask, request, jsonify, Response
from aihandler import ImageClassificationPipeline, createClassifier, predictAnimal
from PIL import Image
import requests
import io

type APIReturn = tuple[Response, int | None]
ALLOWED_EXTENSIONS: set[str] = {'png', 'jpg', 'jpeg'}
GBIF_API_URL: str = "https://api.gbif.org/v1/species/"

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

    scientificName: str = max(prediction, key=prediction.get)
    animalName: str = scientificToCommonName(scientificName)

    return jsonify({
        "success": True,
        "animalName": animalName.lower()
    })

def isFileAllowed(filename: str) -> bool:
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def scientificToCommonName(scientificName: str) -> str:
    try:
        response = requests.get(GBIF_API_URL + "match", params={"name": scientificName}, timeout=5)
        response.raise_for_status()

        usageKey: int = response.json().get("usageKey")
        if not usageKey:
            raise ValueError(f"{scientificName} doesn't exist")
        
        response = requests.get(GBIF_API_URL + str(usageKey) + "/vernacularNames", timeout=5)
        response.raise_for_status()

        for record in response.json().get("results", []):
            if record.get("language") == "eng":
                return record.get("vernacularName")
            
        raise ValueError(f"{scientificName} doesn't have a english common name")

    except (requests.RequestException, ValueError):
        raise ValueError(f"Failed to get {scientificName}'s common name")

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000)
