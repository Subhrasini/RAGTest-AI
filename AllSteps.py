import os
import re
import glob
import time
from langchain_core.documents import Document
from langchain_google_genai import GoogleGenerativeAIEmbeddings, ChatGoogleGenerativeAI
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate
from langchain_community.vectorstores import Chroma
from langchain.schema import StrOutputParser
from dotenv import load_dotenv

# --- CONFIGURATION ---
CODE_FOLDER_PATH  = "./test_code/"
VECTOR_STORE_PATH = "chroma_code_index" # Renamed to avoid confusion with old FAISS index

load_dotenv()

def create_vector_store():
    print(f"Index not found. Starting the indexing process...")
    all_docs_chunks = []

    print(f"Searching for .java files in '{CODE_FOLDER_PATH}'...")
    file_paths = glob.glob(os.path.join(CODE_FOLDER_PATH, '**', '*.java'), recursive=True)
    if not file_paths:
        print(f"Error: No .java files found in '{CODE_FOLDER_PATH}'.")
        return None

    print(f"Found {len(file_paths)} code file(s).")
    for file_path in file_paths:
        print(f"--> Processing file: {file_path}")
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                full_code = f.read()
        except Exception as e:
            print(f"    Warning: Could not read file {file_path}. Error: {e}")
            continue
        
        split_pattern = r'(?=\/\*\*)'
        split_parts = re.split(split_pattern, full_code)

        if split_parts[0].strip():
            all_docs_chunks.append(Document(
                page_content=split_parts[0],
                metadata={"source": os.path.normpath(file_path), "chunk_type": "header"}
            ))
        
        for i in range(1, len(split_parts)):
            if split_parts[i].strip():
                full_chunk_text = "/**" + split_parts[i]
                all_docs_chunks.append(Document(
                    page_content=full_chunk_text,
                    metadata={"source": os.path.normpath(file_path), "chunk_type": "method"}
                ))
    
    if not all_docs_chunks:
        print("Warning: No document chunks were created. Check file contents.")
        return None
    
    print(f"\nSuccessfully created a total of {len(all_docs_chunks)} chunks from all files.")
    
    print("Initializing embedding model...")
    embeddings = GoogleGenerativeAIEmbeddings(model="models/text-embedding-004")
    
    print("Creating vector store from chunks in batches to respect API rate limits...")
    batch_size = 5
    vector_store = None
    
    if all_docs_chunks:
        print(f"--> Processing batch 1/{(len(all_docs_chunks) - 1)//batch_size + 1}...")
        first_batch = all_docs_chunks[:batch_size]
        vector_store = Chroma.from_documents(
            documents=first_batch,
            embedding=embeddings,
            persist_directory=VECTOR_STORE_PATH
        )
        print("    Waiting 61 seconds before the next batch...")
        time.sleep(120)
    
    for i in range(batch_size, len(all_docs_chunks), batch_size):
        batch = all_docs_chunks[i:i + batch_size]
        print(f"--> Processing batch {i//batch_size + 1}/{(len(all_docs_chunks) - 1)//batch_size + 1}...")
        vector_store.add_documents(batch)
        print("    Waiting 61 seconds before the next batch...")
        time.sleep(120)

    if vector_store:
        print("All batches processed. Finalizing the vector store...")
        vector_store.persist()
        print(f"\nChromaDB vector store created and saved successfully to '{VECTOR_STORE_PATH}'.")
    else:
        print("No vector store was created as there were no documents to process.")
        
    return vector_store

def main():
    """Main function to run the RAG application."""
    if "GOOGLE_API_KEY" not in os.environ:
        print("Error: GOOGLE_API_KEY environment variable not set.")
        return

    embeddings = GoogleGenerativeAIEmbeddings(model="models/text-embedding-004")
    if os.path.exists(VECTOR_STORE_PATH):
        print(f"Loading existing ChromaDB vector store from '{VECTOR_STORE_PATH}'...")
        vector_store = Chroma(
            persist_directory=VECTOR_STORE_PATH,
            embedding_function=embeddings
        )
        print("Vector store loaded successfully.")
    else:
        vector_store = create_vector_store()
        if vector_store is None:
            return

    print("Initializing Gemini 1.5 Flash...")
    llm = ChatGoogleGenerativeAI(model="gemini-2.0-flash", temperature=0.1, request_timeout=120)

    # ==================== The Final Chain-of-Thought Prompt ====================
    synthesis_prompt_template = """
    You are an expert Java Test Automation Engineer AI assistant. Your task is to write a new, complete TestNG test method by synthesizing logic from the provided context examples.

    **USER REQUEST:**
    {input}

    **CONTEXT EXAMPLES FROM THE CODEBASE:**
    // Use these examples to find the exact class and method names.
    <context>
    {context}
    </context>
    
    **YOUR THOUGHT PROCESS (Chain-of-Thought):**
    1.  First, I will analyze the user's request to understand the goal.
    2.  Next, I will carefully examine the "CONTEXT EXAMPLES" to identify the relevant classes (e.g., `HoldOperations`), methods (e.g., `Hold_CreateHold`, `Hold_SearchHold`), and the sequence of operations.
    3.  Finally, I will use ONLY the classes and methods found in the context to construct a new, complete `[Test]` method that fulfills the user's request.

    **CRITICAL OUTPUT RULES:**
    1.  You MUST generate ONLY the Java code for the `@Test` method itself. Do not include `import` statements, `package`, `class` definitions, or any explanatory text.
    2.  You MUST use the function and method names **VERBATIM** and **EXACTLY AS THEY APPEAR** in the "CONTEXT EXAMPLES".
    3.  **DO NOT INVENT, INFER, ALTER, OR "CORRECT" ANY METHOD NAMES.** If the context shows a function named `Hold_SearchHold`, you must use `Hold_SearchHold` and not invent something like `Hold_SearchHoldByName`.

    **FINAL Java TEST METHOD (CODE ONLY):**
    """
    synthesis_prompt = ChatPromptTemplate.from_template(synthesis_prompt_template)
    synthesis_document_chain = create_stuff_documents_chain(llm, synthesis_prompt)

    # ==================== NEW: Query Classifier Chain ====================
    classifier_prompt_template = """
    You are a query classifier. Your task is to analyze the user's request and determine if it is a "simple_question" or a "complex_code_generation" request.

    A "simple_question" is a request for information, an explanation, or a request to retrieve a single, existing piece of code.
    Examples:
    - "What is the purpose of TC721004B?"
    - "Show me the test for changing location type"
    - "How does the system handle login?"

    A "complex_code_generation" request asks for the creation of a new test case that likely requires combining logic from multiple different examples. It often involves multiple steps or requirements.
    Examples:
    - "Write a new test that creates an Organization and then logs in as that user"
    - "Generate a test case to create a new record type and then add an additional field to it"
    - "Create a test that sends a record to the recycle bin and then immediately restores it"

    Analyze the following user request and respond with ONLY the category name: "simple_question" or "complex_code_generation". Do not add any other text or explanation.

    User Request:
    {input}

    Category:
    """

    classifier_prompt = ChatPromptTemplate.from_template(classifier_prompt_template)        
    classifier_chain = classifier_prompt | llm | StrOutputParser()
    # ==================== END OF NEW CHAIN ====================

    # ==================== CHANGE #1: ADD THE PLANNER CHAIN ====================
    planner_prompt_template = """
    You are an AI assistant that functions as a "planner" for a code generation system.
    Your task is to analyze a user's request and break it down into a list of simple, highly specific, and searchable sub-queries.

    **RULES:**
    1.  Generate a Python-style list of strings (e.g., `["query 1", "query 2"]`).
    2.  If the request is complex and requires combining information (e.g., creating multiple things, performing multiple steps), create a sub-query for each distinct step.
    3.  If the request is simple (e.g., asking for an explanation, finding one thing), your plan should be a list containing ONLY the original user request.
    
    **Example 1 (Complex):**
    User Request: "write a test case which creates a hold and a location, and then sets access controls on the hold using the location"
    Your Plan:
    ["a test that creates a Hold object", "a test that creates a Location object", "a test that sets access controls ON A HOLD using a Location object"]

    **Example 2 (Simple):**
    User Request: "what is the purpose of TC_246100?"
    Your Plan:
    ["what is the purpose of TC_246100?"]

    **Now, generate the plan for the following user request:**

    User Request: {input}
    Your Plan:
    """
    planner_prompt = ChatPromptTemplate.from_template(planner_prompt_template)
    planner_chain = planner_prompt | llm | StrOutputParser()
    # ============================ END OF PLANNER CHAIN ============================


    print("\n--- Code-Aware RAG Assistant is Ready ---")
    print("Enter your question or request. Type 'exit' to quit.")

    while True:
        user_question = input("\nYour Request: ")
        if user_question.lower() == 'exit':
            break
        print("Thinking...")

        # --- Step 1: ALWAYS generate a plan ---
        print("  -> Generating a retrieval plan...")
        plan_str = planner_chain.invoke({"input": user_question})
        print(f"  -> Generated Plan: {plan_str}")
        
        sub_queries = []
        try:
            sub_queries = eval(plan_str)
            if not isinstance(sub_queries, list):
                sub_queries = [user_question] # Fallback
        except Exception as e:
            sub_queries = [user_question] # Fallback

        # --- Step 2: Execute the retrieval plan ---
        print("  -> Executing retrieval plan...")
        final_context_docs = []
        unique_contents = set()
        
        # We can decide our 'k' value based on the complexity of the plan
        # If the plan has more than one step, it's complex, so we might want fewer, more precise docs.
        k_value = 1 if len(sub_queries) > 1 else 4

        retriever = vector_store.as_retriever(
            search_type="similarity_score_threshold",
            search_kwargs={
                'k': k_value, 
                'score_threshold': 0.5 # Strict quality threshold
            }
        )

        for query in sub_queries:
            print(f"    -> Retrieving for sub-query: '{query}' (k={k_value})")
            retrieved_docs = retriever.invoke(query)
            for doc in retrieved_docs:
                if doc.page_content not in unique_contents:
                    final_context_docs.append(doc)
                    unique_contents.add(doc.page_content)
        
        # --- Context Quality Gate (from our previous fix) ---
        if not final_context_docs:
            print("\n--- Assistant's Answer ---")
            print("I could not find any relevant information or code examples for your query. Please try being more specific.")
            print("-" * 25)
            continue # Skip to the next loop iteration

        print(f"\nCollected {len(final_context_docs)} unique, relevant document(s).")

        # --- Step 3: Execute the final generation ---
        response = synthesis_document_chain.invoke({
            "input": user_question,
            "context": final_context_docs
        })
        print("\n--- Assistant's Answer ---")
        print(response)

        print("-" * 25)

if __name__ == "__main__":
    main()