# RAGTest-AI

An Agent for Your Test Automation

RAGTest-AI helps QA engineers and test automation developers save time, maintain consistency, and automatically extend their test coverage by understanding existing their test automation code/page object model and generating new tests or explanations grounded in the codebase.


## Features


•	Can generate test automation code in any language (like Java/.Net/Python/JavaScript) using any test framework (like TestNG,Nunit,BDD) .

•	Automatic Code Indexing – Scans and chunks .java files intelligently

•	RAG-based Code Retrieval – Uses semantic search via ChromaDB

•	Context-Aware Test Generation – Builds new tests from real examples

•	Smart Query Classification – Distinguishes between search vs generation

•	Multi-Step Planning – Decomposes complex user requests

•	Strict Output Control – Ensures only valid Test Automation code

•	OpenSource and light weight - It takes few minutes to setup this project and get started




## How it works

1. The assistant indexes your automation code into a Chroma vector store.
2. On each request it:
   - Classifies the request (search vs. generation).
   - Generates a plan for complex tasks.
   - Retrieves relevant code snippets from the vector store.
   - Synthesizes an explanation or a new test method (uses Gemini 2.0 Flash).
3. All outputs are grounded in your existing classes and methods for accuracy.



## Project structure

- AllSteps.py – Main orchestration script
- test_code/ – Folder containing your existing test files (Java)
- chroma_code_index/ – Chroma vector store directory (auto-generated)
- .env – Environment file for Google API keys



## Getting started

Prerequisites
- Python 3.9+
- A Google Generative AI key from https://aistudio.google.com/api-keys

1) Create a .env file in the repo root with at least:

```
GOOGLE_API_KEY=your_api_key_here
```

2) Run the below steps:

```bash
python -m venv venv
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\venv\Scripts\Activate
python .\AllSteps.py

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


@Test

public void test_CreateAndVerifyHold() {

    HoldOperations holdOps = new HoldOperations();
    
    holdOps.Hold_CreateHold("Automation_Hold");
    
    holdOps.Hold_SearchHold("Automation_Hold");
    
    holdOps.Hold_VerifyHoldDetails();
}


## Notes & configuration tips

- chroma_code_index is generated when AllSteps.py builds the vector store — remove or recreate it if you change embedding logic.
- Increase/decrease score_threshold to tune retrieval precision.
- Update synthesis_prompt_template to match your preferred test language/framework (TestNG, JUnit, NUnit, PyTest, etc.).

---



