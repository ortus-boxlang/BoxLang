#!/bin/bash

## BOXLANG WEB INSTALLER
main() {
	# Use colors, but only if connected to a terminal, and that terminal
	# supports them.
	if which tput >/dev/null 2>&1; then
		ncolors=$(tput colors)
	fi
	if [ -t 1 ] && [ -n "$ncolors" ] && [ "$ncolors" -ge 8 ]; then
		RED="$(tput setaf 1)"
		GREEN="$(tput setaf 2)"
		YELLOW="$(tput setaf 3)"
		BLUE="$(tput setaf 4)"
		BOLD="$(tput bold)"
		NORMAL="$(tput sgr0)"
	else
		RED=""
		GREEN=""
		YELLOW=""
		BLUE=""
		BOLD=""
		NORMAL=""
	fi

	# Only enable exit-on-error after the non-critical colorization stuff,
	# which may fail on systems lacking tput or terminfo
	set -e

	# Setup Global Variables
	SNAPSHOT_URL="https://downloads.ortussolutions.com/ortussolutions/boxlang/boxlang-snapshot.zip"
	SNAPSHOT_URL_MINISERVER="https://downloads.ortussolutions.com/ortussolutions/boxlang-runtimes/boxlang-miniserver/boxlang-miniserver-snapshot.zip"
	TARGET_VERSION=${1}
	VERSIONED_URL="https://downloads.ortussolutions.com/ortussolutions/boxlang/${TARGET_VERSION}/boxlang-${TARGET_VERSION}.zip"
	VERSIONED_URL_MINISERVER="https://downloads.ortussolutions.com/ortussolutions/boxlang-runtimes/boxlang-miniserver/${TARGET_VERSION}/boxlang-miniserver-${TARGET_VERSION}.zip"
	DESTINATION="/usr/local/"
    DESTINATION_LIB="/usr/local/lib"
    DESTINATION_BIN="/usr/local/bin"

	# Determine which URL to use
	if [[ ${TARGET_VERSION} == "snapshot" ]]; then
		DOWNLOAD_URL=${SNAPSHOT_URL}
		DOWNLOAD_URL_MINISERVER=${SNAPSHOT_URL_MINISERVER}
	else
		DOWNLOAD_URL=${VERSIONED_URL}
		DOWNLOAD_URL_MINISERVER=${VERSIONED_URL_MINISERVER}
	fi

	# Check java exists
	command -v java >/dev/null 2>&1 || {
		echo "Error: Java is not installed and we need it for BoxLang to work. Please download JDK 17+ from https://adoptopenjdk.net/ and install it."
		exit 1
	}

	# Check java version is 17 or higher
	JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
	if [[ ${JAVA_VERSION} < "17" ]]; then
		echo "Error: Java 17 or higher is required to run BoxLang"
		exit 1
	fi

	# Check curl exists
	command -v curl >/dev/null 2>&1 || {
		echo "Error: curl is not installed and we need it in order for the quick installer to work"
		exit 1
	}

	# Tell them where we will install
	printf "${GREEN}"
	echo ''
	echo '*************************************************************************'
	echo 'Welcome to the BoxLang® Quick Installer'
	echo '*************************************************************************'
	echo 'This will download and install the latest version of BoxLang® and the'
	echo 'BoxLang® MiniServer into your system.'
	echo '*************************************************************************'
	echo 'You can also download the BoxLang® runtimes from https://boxlang.io'
	echo '*************************************************************************'
	printf "${NORMAL}"

	# Announce it
	printf "${BLUE}Downloading BoxLang® [${TARGET_VERSION}] from [${DOWNLOAD_URL}]${NORMAL}\n"
	printf "${RED}Please wait...${NORMAL}\n"

	# Ensure destination folders
	mkdir -p /tmp
	mkdir -p /usr/local/bin
	mkdir -p /usr/local/lib

	# Download
	env curl -Lk -o /tmp/boxlang.zip -location ${DOWNLOAD_URL} || {
		printf "Error: Download of BoxLang® binary failed\n"
		exit 1
	}
	env curl -Lk -o /tmp/boxlang-miniserver.zip -location ${DOWNLOAD_URL_MINISERVER} || {
		printf "Error: Download of BoxLang® MiniServer binary failed\n"
		exit 1
	}
	printf "\n"
	printf "${GREEN}BoxLang® downloaded to [/tmp/boxlang.zip, /tmp/boxlang-miniserver.zip], continuing installation...${NORMAL}\n"
	printf "\n"

	# Inflate it
	printf "\n"
	printf "${BLUE}Unziping BoxLang®...${NORMAL}\n"
	printf "\n"
	unzip -o /tmp/boxlang.zip -d ${DESTINATION}
	unzip -o /tmp/boxlang-miniserver.zip -d ${DESTINATION}

	# Make it executable
	printf "\n"
	printf "${BLUE}Making BoxLang® Executable...${NORMAL}\n"
	chmod +x ${DESTINATION_BIN}/boxlang
	chmod +x ${DESTINATION_BIN}/boxlang-miniserver
	# Add ln to bx
	ln -s ${DESTINATION_BIN}/boxlang ${DESTINATION_BIN}/bx
	ln -s ${DESTINATION_BIN}/boxlang-miniserver ${DESTINATION_BIN}/bx-miniserver


	# Run it
	printf "${BLUE}Testing BoxLang®...${NORMAL}\n"
	printf "\n"
	${DESTINATION_BIN}/boxlang --version

	printf "${GREEN}"
	echo ''
	echo "BoxLang® Binaries are now installed to [$DESTINATION_BIN]"
	echo "BoxLang® JARs are now installed to [$DESTINATION_LIB]"
	echo "BoxLang® Home is now set to [~/.boxlang]"
	echo ''
	echo ''Your [BOXLANG_HOME] is set by default to your user home directory.
	echo ''You can change this by setting the [BOXLANG_HOME] environment variable in your shell profile
	echo ''Just copy the following line to override the location if you want
	echo ''
	echo "EXPORT BOXLANG_HOME=/opt/boxlang"
	echo ''
	echo "You can start a MiniServer by running: boxlang-miniserver"
	echo '*************************************************************************'
	echo 'BoxLang® - Dynamic : Modular : Productive : https://boxlang.io'
	echo '*************************************************************************'
	echo "BoxLang® is FREE and Open-Source Software under the Apache 2.0 License"
	echo "You can also buy support and enhanced versions at https://boxlang.io/plans"
	echo 'p.s. Follow us at https://twitter.com/ortussolutions.'
	echo 'p.p.s. Clone us and star us at https://github.com/ortus-boxlang/boxlang'
	echo 'Please support us via Patreon at https://www.patreon.com/ortussolutions'
	echo '*************************************************************************'
	echo "Copyright and Registered Trademarks of Ortus Solutions, Corp"
	printf "${NORMAL}"

}

main ${1:-snapshot}
