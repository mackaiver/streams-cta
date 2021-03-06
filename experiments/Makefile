SHELL=/bin/bash

jar = ../target/streams-cta-0.0.3-SNAPSHOT-stream-compiled.jar
xml = process_data.xml
raw_data_folder = /fhgfs/users/kbruegge/cta

gamma_output = ../data/image_features/gammas.csv
proton_output = ../data/image_features/protons.csv

proton_test = ../data/image_features/protons_test.csv
gamma_test = ../data/image_features/gammas_test.csv

proton_train = ../data/image_features/protons_train.csv
gamma_train = ../data/image_features/gammas_train.csv

all: $(proton_test) $(proton_train) $(gamma_test) $(gamma_train) ../build/ANGULAR ../build/COLLECTION ../build/ML_PERF ../build/SENSITIVITY

$(proton_test) $(proton_train): $(proton_output)
	klaas_split_data $(proton_output) ../data/image_features/protons -n test -f 0.6 -n train -f 0.4 --fmt csv -id unique_event_id

$(gamma_test) $(gamma_train): $(gamma_output)
	klaas_split_data $(gamma_output) ../data/image_features/gammas -n test -f 0.6 -n train -f 0.4 --fmt csv -id unique_event_id


$(gamma_output): $(jar) $(xml)
	java  -jar $(jar) $(xml) -Dprefix="gamma_20deg_0deg_run*.json.gz" -Dfolder=$(raw_data_folder) -Doutfile=$(gamma_output)

$(proton_output): $(jar) $(xml)
	java  -jar $(jar) $(xml) -Dprefix="proton_20deg_0deg_run*.json.gz" -Dfolder=$(raw_data_folder) -Doutfile=$(proton_output)

../build/ANGULAR: $(gamma_test)
	cd angular_resolution;MATPLOTLIBRC=../ make; cd ..

../build/COLLECTION: $(gamma_test) $(proton_test)
	cd collection_area;MATPLOTLIBRC=../ make; cd ..

../build/ML_PERF:
	cd ml_performance;MATPLOTLIBRC=../ make; cd ..

../build/SENSITIVITY: $(gamma_test) $(proton_test)
	cd sensitivity;MATPLOTLIBRC=../ make; cd ..

$(jar): ../pom.xml $(shell find ../src -type f)
	echo "building streams package in top level"
	cd ../; mvn -P standalone package -DskipTests; cd -
