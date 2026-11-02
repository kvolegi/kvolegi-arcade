.PHONY: help test jar run clean

help: ## Show this help message
	@echo "=================================================================="
	@echo " kvolegi arcade — Developer & Build Orchestration Makefile"
	@echo "=================================================================="
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-16s\033[0m %s\n", $$1, $$2}'

test: ## Execute the headless automated test suite (14 tests)
	@echo "==> Running automated headless test suite..."
	python3 tests/run_tests.py

jar: ## Build the cross-platform universal fat-JAR
	@echo "==> Building kvolegi-arcade-universal.jar..."
	python3 build_all.py

run: ## Launch the universal executable JAR
	@if [ -f builds/kvolegi-arcade-universal.jar ]; then \
		echo "==> Launching builds/kvolegi-arcade-universal.jar..."; \
		java -jar builds/kvolegi-arcade-universal.jar; \
	else \
		echo "==> Binary not found. Building first..."; \
		$(MAKE) jar; \
		java -jar builds/kvolegi-arcade-universal.jar; \
	fi

clean: ## Clean generated classes and temporary build caches
	@echo "==> Cleaning build artifacts..."
	rm -rf source/core/arcade/*.class source/desktop/arcade/desktop/*.class
