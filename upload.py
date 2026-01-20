import subprocess
import os

# --- Configuration ---
# The directory where your project is located
PROJECT_DIRECTORY = "/Users/darrenfernandez/train_app"
# The commit message
COMMIT_MESSAGE = "Initial commit"
# The name for your GitHub repository
REPO_NAME = "train_app"

def run_command(command, cwd):
    """Runs a shell command and checks for errors."""
    print(f"--- Running: {' '.join(command)} ---")
    try:
        subprocess.run(
            command,
            cwd=cwd,
            check=True,
            capture_output=True,
            text=True
        )
    except FileNotFoundError:
        print(f"Error: Command '{command[0]}' not found. Please ensure Git is installed.")
        exit(1)
    except subprocess.CalledProcessError as e:
        print(f"Error executing command: {' '.join(command)}")
        print(e.stderr)
        exit(1)

if __name__ == "__main__":
    # 1. Initialize a new Git repository
    run_command(["git", "init"], cwd=PROJECT_DIRECTORY)

    # 2. Add all files to the staging area
    run_command(["git", "add", "."], cwd=PROJECT_DIRECTORY)

    # 3. Commit the files
    run_command(["git", "commit", "-m", COMMIT_MESSAGE], cwd=PROJECT_DIRECTORY)

    print("\n--- Local repository is ready! ---")
    print("\nTo complete the process, run this final command in your terminal:")
    print(f"gh repo create {REPO_NAME} --public --source=. --push")
