### Get plugins
curl -u $JENKINS_USERNAME:$JENKINS_TOKEN -sSL "https://$JENKINS_HOSTNAME/pluginManager/api/xml?depth=1&xpath=/*/*/shortName|/*/*/version&wrapper=plugins" | perl -pe 's/.*?<shortName>([\w-]+).*?<version>([^<]+)()(<\/\w+>)+/\1 \2\n/g'|sed 's/ /:/'

### Build
docker build -t jenkins:test .

### Setup local bind mount
mkdir jenkins_home && chown -R 1000:1000 jenkins_home

### Run
docker run -d -p 8080:8080 -p 50000:50000 \
  -v `pwd`/jenkins_home:/var/jenkins_home \
  --name jenkins \
  jenkins:test

