# RAGTest-AI

An Agent for Your Test Automation

RAGTest-AI helps QA engineers and test automation developers save time, maintain consistency, and automatically extend their test coverage by understanding existing Java test automation code and generating new tests or explanations grounded in the codebase.


## Features

- Retrieval-Augmented Generation (RAG) for code-aware responses
- Semantic code search with ChromaDB
- Automatic indexing and intelligent chunking of .java files
- Context-aware test generation (ensures correct names/signatures)
- Multi-step planning for complex requests
- Strict output control to produce valid test methods only

---

## How it works

1. The assistant indexes your automation code into a Chroma vector store.
2. On each request it:
   - Classifies the request (search vs. generation).
   - Generates a plan for complex tasks.
   - Retrieves relevant code snippets from the vector store.
   - Synthesizes an explanation or a new test method (uses Gemini 2.0 Flash).
3. All outputs are grounded in your existing classes and methods for accuracy.

---

## Project structure

- AllSteps.py – Main orchestration script
- test_code/ – Folder containing your existing test files (Java)
- chroma_code_index/ – Chroma vector store directory (auto-generated)
- .env – Environment file for API keys

---

## Getting started

Prerequisites
- Python 3.9+
- A Google Cloud project with a Generative AI key

1) Create a .env file in the repo root with at least:

```
GOOGLE_API_KEY=your_api_key_here
```

2) Install dependencies (example):

```bash
python -m venv venv
source venv/bin/activate  # on Windows: .\venv\Scripts\Activate
pip install -r requirements.txt
```

3) Configure AllSteps.py variables as needed (defaults shown):

- CODE_FOLDER_PATH: ./test_code/
- VECTOR_STORE_PATH: chroma_code_index
- batch_size: 5
- score_threshold: 0.5

Adjust synthesis_prompt_template for your target language/framework if required.

4) Run the assistant:

```bash
python AllSteps.py
```
---

## Example inputs

- "Generate a new test to verify login with invalid credentials"
- "Explain what TC_721004B does"
- "Create a test that creates a Hold and then deletes it"

## Example output (Java)

```java
@Test
public void test_CreateAndVerifyHold() {
    HoldOperations holdOps = new HoldOperations();
    holdOps.Hold_CreateHold("Automation_Hold");
    holdOps.Hold_SearchHold("Automation_Hold");
    holdOps.Hold_VerifyHoldDetails();
}
```
---

## Notes & configuration tips

- chroma_code_index is generated when AllSteps.py builds the vector store — remove or recreate it if you change embedding logic.
- Increase/decrease score_threshold to tune retrieval precision.
- Update synthesis_prompt_template to match your preferred test language/framework (TestNG, JUnit, NUnit, PyTest, etc.).

---



