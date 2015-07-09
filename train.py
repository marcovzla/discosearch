#!/usr/bin/env python

import sys
sys.path.append('/Users/marcov/repos/vowpal_wabbit/python')

import pyvw
import json
import random
import requests

class DiscoSession:
    disco_url = 'http://localhost:8080/'

    # keep session to reuse connection (keep-alive)
    session = requests.Session()

    def get_doc_ids(self):
        r = self.session.get(self.disco_url + 'get_doc_ids')
        return r.json()

    def start_parse(self, id):
        r = self.session.get(self.disco_url + 'start_parse', params=dict(id=id))
        return r.json()

    def get_tree(self):
        r = self.session.get(self.disco_url + 'get_tree')
        return r.json()

    def is_done(self):
        r = self.session.get(self.disco_url + 'is_done')
        return r.json()['done']

    def get_valid_actions(self):
        r = self.session.get(self.disco_url + 'get_valid_actions')
        return r.json()

    def perform_action(self, action):
        r = self.session.post(self.disco_url + 'perform_action', json=action)
        return r.json()

    def get_loss(self):
        r = self.session.get(self.disco_url + 'get_loss')
        return r.json()['loss']

class DiscourseParser(pyvw.SearchTask):
    STRUCTURE = 1
    LABEL = 2
    NUCLEUS = 3

    def __init__(self, vw, sch, num_actions):
        pyvw.SearchTask.__init__(self, vw, sch, num_actions)
        self.disco = DiscoSession()

    def _run(self, doc_id):
        self.disco.start_parse(doc_id)
        output = []
        n = 1
        while not self.disco.is_done():
            actions = self.disco.get_valid_actions()
            features = self.disco.get_features()
            ref = self.disco.get_gold_action()
            with self.vw.example(features) as ex:
                pred = self.sch.predict(examples=ex, my_tag=n, oracle=ref, allowed=actions, learner_id=STRUCTURE)
                self.disco.perform_action(pred)
                output.append(pred)
                n += 1
        self.sch.loss(self.disco.get_loss())
        return output

if __name__ == '__main__':
    disco = DiscoSession()
    for id in disco.get_doc_ids():
        disco.start_parse(id)
        print 'parsing', id
        while not disco.is_done():
            actions = disco.get_valid_actions()
            action = random.choice(actions)
            disco.perform_action(action)
            print action
        print 'loss =', disco.get_loss()
        print '=' * 70
