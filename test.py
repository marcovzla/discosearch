#!/usr/bin/env python

import sys
sys.path.append('/Users/marcov/repos/vowpal_wabbit/python')

import pyvw
from train import DiscourseParser, DiscoSession

if __name__ == '__main__':
    disco = DiscoSession()
    dataset = disco.get_doc_ids()

    vw = pyvw.vw("--quiet -i disco.vw")
    parser = vw.init_search_task(DiscourseParser)

    print 'testing ...'

    for d in dataset:
        parser.predict(d)

    vw.finish()
    print 'done!'
