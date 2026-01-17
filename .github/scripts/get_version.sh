GRADLEW_OUTPUT=$(./gradlew printVersion -q)
if [ $? != 0 ]
then
  echo "Could not get version, probably missing printVersion gradlew task"
  exit 1
fi
VERSION=$(echo "$GRADLEW_OUTPUT" | grep "MOD_VERSION" | cut -d'=' -f2)
echo "VERSION = $VERSION"
echo "version=$VERSION" >> "$GITHUB_OUTPUT"
echo "display_version=${VERSION//-/ - }" >> "$GITHUB_OUTPUT"
