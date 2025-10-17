RAGTest-AI 
An Agent for Your Test Automation

RAGTest-AI helps QA engineers and test automation developers save time, maintain consistency, and automatically extend their test coverage.

🧠 Overview
RAGTest-AI is an intelligent assistant that understands your Java test automation code.

It uses Retrieval-Augmented Generation (RAG) to:

🔍 Search through your codebase

💬 Answer natural-language questions about existing tests

🧪 Generate new @Test / [Test] methods using context from your existing code

🚀 Key Features
🌐 Generates test automation code in any language (Java, .NET, Python, JavaScript) using any framework (TestNG, NUnit, BDD)

📁 Automatic Code Indexing – Scans and chunks .java files intelligently

🔎 RAG-based Code Retrieval – Uses semantic search via ChromaDB

🧠 Context-Aware Test Generation – Builds new tests from real examples

🧭 Smart Query Classification – Distinguishes between search vs generation

🧩 Multi-Step Planning – Decomposes complex user requests

✅ Strict Output Control – Ensures only valid Java @Test method code

👐 Open Source

⚙️ How It Works
Loads your existing automation test code into a vector store (Chroma).

Upon receiving a request:

Classifies it (simple vs complex)

Generates a plan (if complex)

Retrieves relevant code snippets

Synthesizes a new test or explanation using Gemini 2.0 Flash

Each response is grounded in your existing classes and methods, ensuring naming accuracy and context alignment.

📁 Project Structure
File / Folder	Description
AllSteps.py	Main orchestration script
test_code/	Folder containing your existing test files
chroma_code_index/	Vector store directory
.env	Contains your Google API key
🛠️ Getting Started
1. Prerequisites
Python 3.9+

A Google Cloud project with a Generative AI key

.env file containing:

env
GOOGLE_API_KEY=your_api_key_here
2. Install Dependencies
Make sure Python is installed on your machine.

3. Configuration in AllSteps.py
Variable	Description	Default
CODE_FOLDER_PATH	Directory containing .java test files	./test_code/
VECTOR_STORE_PATH	Directory for Chroma vector index	chroma_code_index
batch_size	Number of files per embedding batch	5
score_threshold	Similarity filter for retrieval	0.5
./test_code/ – Add existing automation scripts

chroma_code_index – Automatically generated when AllSteps.py is executed

score_threshold – Can be adjusted based on results

synthesis_prompt_template – Update according to your test language/framework

4. Run the Assistant
bash
python -m venv venv
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\venv\Scripts\Activate
python .\AllSteps.py
💡 Example Inputs
Generate a new test to verify login with invalid credentials

Explain what TC_721004B does

Create a test that creates a Hold and then deletes it

✅ Example Output
java
@Test
public void test_CreateAndVerifyHold() {
    HoldOperations holdOps = new HoldOperations();
    holdOps.Hold_CreateHold("Automation_Hold");
    holdOps.Hold_SearchHold("Automation_Hold");
    holdOps.Hold_VerifyHoldDetails();
}
