from flask import Flask, jsonify, Response

app = Flask(__name__)

@app.route("/")
def homepage() -> Response:
    return jsonify({
        "success": True
    })

@app.route("/getstats/<animalName>")
def getStats(animalName: str) -> Response:
    return jsonify({
        "name": animalName,
        "hp": 0,
        "atk": 0,
        "def": 0
    })

if __name__ == '__main__':
    app.run(debug=True, port=5000)
