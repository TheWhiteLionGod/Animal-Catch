from transformers import pipeline, ImageClassificationPipeline
from PIL import Image

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
