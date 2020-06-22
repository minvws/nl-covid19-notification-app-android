#!/usr/bin/env bash

if [ "$PLAY_STORE_JSON" != "" ]; then
  if [ "$AGENT_JOBSTATUS" == "Succeeded" ]; then
    if [ "$APPCENTER_BRANCH" == "master" ]; then
      cd ..
      echo "$PLAY_STORE_JSON" >google-play.json
      sed -i -e 's/\\"/'\"'/g' google-play.json
      ./gradlew app:publishBundle
      RESULT=$?
      rm google-play.json
      exit $RESULT
    fi
  fi
fi
