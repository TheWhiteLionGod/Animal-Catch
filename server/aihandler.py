from transformers import pipeline, ImageClassificationPipeline
from PIL import Image
import time
import os

def createClassifier() -> ImageClassificationPipeline:
    try:
        return pipeline(
            "image-classification", 
            model="timm/vit_large_patch14_clip_336.laion2b_ft_augreg_inat21",
            device="cuda",
            model_kwargs={"dtype": "auto", "low_cpu_mem_usage": True}
        )
    
    except Exception:
        return pipeline(
            "image-classification", 
            model="timm/vit_large_patch14_clip_336.laion2b_ft_augreg_inat21",
            device="cpu",
            model_kwargs={"low_cpu_mem_usage": True}
        )

def predictAnimal(classifier: ImageClassificationPipeline, image: Image) -> dict[str, float]:
    predictions: list[dict] = classifier(image)
    return {pred["label"]: pred['score'] for pred in predictions}

def grabPrompt(filename: str) -> str:
    if not os.path.exists(f"prompts/{filename}.txt"):
        raise FileNotFoundError(f"Unable to Find File prompts/{filename}.txt")
    
    with open(f"prompts/{filename}.txt", 'r') as f:
        return f.read()

if __name__ == '__main__':
    # TODO: Delete Testing Code & deer.jpeg
    start: float = time.perf_counter()
    classifier = createClassifier()
    
    end: float = time.perf_counter()
    print(f"Time to Load Model: {end - start:.2f}")
    
    image: Image = Image.open("deer.jpeg").convert("RGB")

    start: float = time.perf_counter()
    predictions = predictAnimal(classifier, image)
    end: float = time.perf_counter()

    print(predictions)
    print(f"Time to Make Prediction: {end - start:.2f}")

    prompt: str = grabPrompt("test")
    print(prompt)
