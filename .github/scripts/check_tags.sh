TAGS=$(git tag | grep "$1")
GREP_EXIT="$?"
echo "TAGS = $TAGS, GREP_EXIT = $GREP_EXIT"
if [ $GREP_EXIT = 0 ]
then
  echo "Tag already exists"
  exit 1
else
  echo "Tag unused, all clear"
  exit 0
fi
