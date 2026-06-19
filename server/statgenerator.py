from transformers import pipeline, TextGenerationPipeline
import json

def createGenerator() -> TextGenerationPipeline:
    try:
        return pipeline(
            "text-generation", 
            model="Qwen/Qwen2.5-1.5B-Instruct",
            device="cuda",
            model_kwargs={"dtype": "auto", "low_cpu_mem_usage": True}
        )
    
    except Exception:
        return pipeline(
            "text-generation", 
            model="Qwen/Qwen2.5-1.5B-Instruct",
            device="cpu",
            model_kwargs={"low_cpu_mem_usage": True}
        )

def generateStats(generator: TextGenerationPipeline, animalName: str) -> dict[str, str | int]:
    messages: list[dict] = [
        {
            "role": "system", 
            "content": 
                """
                You are an RPG game balancing engine. 
                You must output data strictly as a single JSON object. 
                Do not include any thinking, markdown blocks, or extra text.
                """.strip()
        },
        {
            "role": "user", 
            "content": (
                f"Generate RPG stats (1-100) for: {animalName}."
                "Respond ONLY with a valid JSON object matching this structure: "
                '{"name": string, "hp": int, "atk": int, "def": int, "spd": int}'
            )
        }
    ]

    outputs: list[dict] = generator(messages, max_new_tokens=50, temperature=0.1, do_sample=False, return_full_text=False)
    raw_text: str = outputs[0]['generated_text'].strip()
    
    if raw_text.startswith("```json"):
        raw_text = raw_text.split("```json")[1].split("```")[0].strip()
    elif raw_text.startswith("```"):
        raw_text = raw_text.split("```")[1].split("```")[0].strip()

    try:
        return json.loads(raw_text)
    except json.JSONDecodeError:
        raise ValueError(f"Failed to generate stats for {animalName}")
