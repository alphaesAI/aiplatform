# Contributing to AI Platform

Thank you for your interest in contributing to the AI Platform! This document provides guidelines and instructions for setting up the repository on your local system.

## Getting Started

### Prerequisites

Before you begin, make sure you have the following installed on your system:

- **Python 3.8 or higher**: Download from [python.org](https://www.python.org/downloads/)
- **Git**: Download from [git-scm.com](https://git-scm.com/)
- **pip**: Usually comes with Python. Verify by running `pip --version`
- **Virtual Environment**: We recommend using `venv` or `conda` for dependency management

### Step 1: Clone the Repository

Clone the repository to your local machine:

```bash
git clone https://github.com/alphaesAI/aiplatform.git
cd aiplatform
```

If the repository has git submodules (such as the `src/txtai` submodule), initialize them:

```bash
git submodule update --init --recursive
```

### Step 2: Create a Virtual Environment

It's best practice to create an isolated Python environment for this project:

**Using venv:**
```bash
python -m venv venv

# On macOS/Linux:
source venv/bin/activate

# On Windows:
venv\Scripts\activate
```

**Using conda:**
```bash
conda create -n aiplatform python=3.8
conda activate aiplatform
```

### Step 3: Install Dependencies

Install all required dependencies from `requirements.txt`:

```bash
pip install -r requirements.txt
```

To upgrade pip before installing dependencies (recommended):
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### Step 4: Verify Installation

To verify that everything is set up correctly, you can run the test suite:

```bash
python -m pytest tests/
```

## Project Structure

The AI Platform consists of several key components:

- **`src/custom/`**: Core ETL framework including:
  - `connectors/`: Data source connections (Arxiv, Elasticsearch, Gmail, OpenSearch, RDBMS)
  - `credentials/`: Credential management
  - `extractors/`: Data extraction logic
  - `loaders/`: Data loading to target systems
  - `transformers/`: Data transformation pipeline
  - `utils/`: Helper utilities

- **`src/txtai/`**: Git submodule for txtai (AI framework for semantic search and embeddings)

- **`dags/`**: Apache Airflow DAGs for orchestrating data pipelines
  - `structure/health/`: PostgreSQL to Elasticsearch pipeline
  - `unstructure/gmail/`: Gmail data extraction and processing
  - `unstructure/arxiv/`: Arxiv metadata and PDF processing

- **`tests/`**: Unit tests for custom framework components

## Development Workflow

### Making Changes

1. Create a new branch for your feature or bug fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and test them thoroughly

3. Run tests to ensure nothing is broken:
   ```bash
   python -m pytest tests/ -v
   ```

### Committing Your Work

Write clear, descriptive commit messages:

```bash
git add .
git commit -m "Add feature: description of your changes"
```

### Pushing and Creating a Pull Request

Push your branch to GitHub:

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub with a clear description of your changes.

## Dependencies

The project uses the following key dependencies:

- **Apache Airflow**: Data pipeline orchestration
- **Elasticsearch/OpenSearch**: Search and analytics engine
- **txtai**: AI framework for semantic search and embeddings
- **pytest**: Testing framework
- **requests**: HTTP client library

For a complete list, see `requirements.txt`.

## Setting Up for Specific Use Cases

### Running Airflow Locally

If you want to run Apache Airflow locally:

```bash
# Initialize the Airflow database
airflow db init

# Create an admin user
airflow users create \
  --username admin \
  --firstname Admin \
  --lastname User \
  --role Admin \
  --email admin@example.com

# Start the Airflow webserver
airflow webserver --port 8080

# Start the Airflow scheduler (in another terminal)
airflow scheduler
```

The Airflow web UI will be available at `http://localhost:8080`

### Running Tests

Execute the test suite:

```bash
# Run all tests
python -m pytest tests/

# Run tests with verbose output
python -m pytest tests/ -v

# Run specific test file
python -m pytest tests/test_specific.py

# Run tests with coverage report
pytest --cov=src tests/
```

## Configuration

The platform uses YAML configuration files for data pipelines. Configuration files are typically located in:

- `dags/structure/health/config/`: Configuration for structured data pipelines
- `dags/unstructure/*/config/`: Configuration for unstructured data pipelines

Modify these files according to your data sources and target systems.

## Troubleshooting

### Common Issues

**1. Virtual environment not activating:**
- Ensure you're in the project directory
- Check that the venv folder exists
- Try recreating the virtual environment

**2. Import errors after installation:**
```bash
# Reinstall all dependencies
pip install --force-reinstall -r requirements.txt
```

**3. Submodule issues:**
```bash
# Ensure submodules are initialized
git submodule update --init --recursive
```

**4. Airflow errors:**
```bash
# Reset Airflow database
rm ~/airflow/airflow.db
airflow db init
```

## Code Standards

- Follow PEP 8 style guidelines for Python code
- Write meaningful variable and function names
- Add docstrings to functions and classes
- Include type hints where applicable
- Write unit tests for new functionality

## Getting Help

If you encounter issues or have questions:

1. Check the existing GitHub issues
2. Review the README.md for project overview
3. Check the documentation in the `docs/` directory
4. Open a new GitHub issue with detailed information

## License

By contributing to this project, you agree that your contributions will be licensed under the MIT License. See the LICENSE file for details.

Thank you for contributing to AI Platform!
