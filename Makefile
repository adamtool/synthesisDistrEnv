#### @author Manuel Gieseking
# dependencies (folders and repos should be equally ordered)
DEPENDENCIES_FOLDERS="libs,framework,synthesizer"
DEPENDENCIES_REPOS="git@github.com:adamtool/libs.git,git@github.com:adamtool/framework.git,git@github.com:adamtool/synthesizer.git"
DEPENDENCIES_REV="HEAD,HEAD,HEAD"
# the build target
FRAMEWORK_TARGETS = tools petrinetwithtransits
SYNTHESIZER_TARGETS = petrigames symbolic distrEnv
t=jar

# should be executed no matter if a file with the same name exists or not
.PHONY: check_dependencies
.PHONY: pull_dependencies
.PHONY: rm_dependencies
.PHONY: tools
.PHONY: petrinetwithtransits
.PHONY: petrigames
.PHONY: symbolic
.PHONY: bdd
.PHONY: mtbdd
.PHONY: distrEnv
#.PHONY: javadoc
.PHONY: setStandalone
.PHONY: setClean
.PHONY: setCleanAll
.PHONY: clean
.PHONY: clean-all
.PHONY: src_withlibs
.PHONY: src

define generate_src
	mkdir -p adam_src
	if [ $(1) = true ]; then\
		cp -R ./dependencies/libs ./adam_src/libs/; \
		rm -rf ./adam_src/libs/.git; \
	fi
	for i in $$(find . -type d \( -path ./benchmarks -o -path ./test/lib -o -path ./lib -o -path ./adam_src -o -path ./dependencies -o -path ./.git \) -prune -o -name '*' -not -regex ".*\(class\|qcir\|pdf\|tex\|apt\|dot\|jar\|ods\|txt\|tar.gz\|aux\|log\|res\|aig\|aag\|lola\|cex\|properties\|json\|xml\|out\|pnml\|so\)" -type f); do \
		echo "cp" $$i; \
		cp --parent $$i ./adam_src/ ;\
	done
	tar -zcvf adam_src.tar.gz adam_src
	rm -r -f ./adam_src
endef

# targets
all: $(FRAMEWORK_TARGETS) $(SYNTHESIZER_TARGETS)

check_dependencies:
	@if [ ! -d "dependencies" ]; then \
		echo "The dependencies folder is missing. Please execute make pull_dependencies first.";\
	fi

pull_dependencies:
	./pull_dependencies.sh ${DEPENDENCIES_FOLDERS} ${DEPENDENCIES_REPOS} ${DEPENDENCIES_REV}

rm_dependencies:
	rm -rf dependencies

tools: check_dependencies
	ant -buildfile ./dependencies/framework/tools/build.xml $(t)

petrinetwithtransits: check_dependencies
	ant -buildfile ./dependencies/framework/petrinetWithTransits/build.xml $(t)

petrigames:
	ant -buildfile ./petriGames/build.xml $(t)

bdd: check_dependencies
	ant -buildfile ./symbolicalgorithms/bddapproach/build.xml $(t)

mtbdd: check_dependencies
	ant -buildfile ./symbolicalgorithms/mtbddapproach/build.xml $(t)

symbolic: bdd mtbdd

distrEnv:
	ant -buildfile ./build.xml $(t)

setStandalone:
	$(eval t=jar-standalone)

setClean:
	$(eval t=clean)

setCleanAll:
	$(eval t=clean-all)

clean: setClean $(FRAMEWORK_TARGETS) $(SYNTHESIZER_TARGETS)
	rm -r -f deploy
	rm -r -f javadoc

clean-all: setCleanAll $(FRAMEWORK_TARGETS) $(SYNTHESIZER_TARGETS)
	rm -r -f deploy
	rm -r -f javadoc

#javadoc:
#	ant javadoc

src_withlibs: clean-all
	$(call generate_src, true)

src: clean-all
	$(call generate_src, false)
