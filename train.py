#!/usr/bin/env python

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
        print '=' * 70
