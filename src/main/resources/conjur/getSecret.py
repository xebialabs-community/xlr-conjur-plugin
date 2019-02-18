#
# THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
# FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
#

from conjur.core.client import ConjurClient
import sys
import logging

class Container(object):
    pass

logging.basicConfig(filename='log/plugin.log',
                            filemode='a',
                            format='%(asctime)s,%(msecs)d %(name)s %(levelname)s %(message)s',
                            datefmt='%H:%M:%S',
                            level=logging.DEBUG)

if conjurServer is None:
    logging.debug("No server provided.")
    sys.exit(1)
else:
    container = Container()
    container.url = conjurServer["url"]
    container.account = conjurServer["account"]
    container.username = conjurServer["username"]
    container.password = conjurServer["password"]

    conjur = ConjurClient.new_instance(container)
    secretOutput = conjur.retrieve_secret(secretName)



