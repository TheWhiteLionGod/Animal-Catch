from flask import Flask, jsonify, Response

app = Flask(__name__)

@app.route("/")
def homepage() -> Response:
    return jsonify({
        "success": True
    })

if __name__ == '__main__':
    app.run(debug=True, port=5000)
