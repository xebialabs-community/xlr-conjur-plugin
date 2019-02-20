#
# Copyright 2019 XEBIALABS
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

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