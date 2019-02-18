# Copyright (c) 2019 XebiaLabs

#

# This software is released under the MIT License.

# https://opensource.org/licenses/MIT

# conjur/control/check_connection.py

from conjur.core.client import ConjurClient
import logging

logging.basicConfig(filename='log/plugin.log',
                            filemode='a',
                            format='%(asctime)s,%(msecs)d %(name)s %(levelname)s %(message)s',
                            datefmt='%H:%M:%S',
                            level=logging.DEBUG)

class Container(object):
    pass


def process(task_vars):

    '''
    From my xld check connection
    server = task_vars['thisCi']

    conjur = ConjurClient.new_instance(server)
    
    print "Done"
    '''

    conf = task_vars['configuration'] 

    # The configuration we get here is a delegate to an underlying java class.  This is

    # different than the 'server' we get defined in the synthetic.xml.  Convert it to

    # look like the 'server' object.
    
    container = Container()
    container.url = conf._delegate.getUrl()
    container.account = conf._delegate.getProperty("account")
    container.username = conf._delegate.getUsername()
    container.password = conf._delegate.getPassword()

    conjur = ConjurClient.new_instance(container)

if __name__ == '__main__' or __name__ == '__builtin__':
    process(locals())