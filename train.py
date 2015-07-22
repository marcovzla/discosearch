#!/usr/bin/env python

import sys
sys.path.append('/Users/marcov/repos/vowpal_wabbit/python')

import pyvw
import ujson
import random
import requests

class DiscoSession:
    disco_url = 'http://localhost:8080/'

    # keep session to reuse connection (keep-alive)
    session = requests.Session()

    def get_doc_ids(self):
        r = self.session.get(self.disco_url + 'get_doc_ids')
        return ujson.loads(r.text)
        return r.json()

    def start_parse(self, id):
        params = dict(id=id)
        r = self.session.get(self.disco_url + 'start_parse', params=params)
        return ujson.loads(r.text)['msg']
        return r.json()['msg']

    def stop_parse(self, uuid):
        params = dict(uuid=uuid)
        r = self.session.get(self.disco_url + 'stop_parse', params=params)
        return ujson.loads(r.text)['msg']
        return r.json()['msg']

    def get_tree(self, uuid):
        params = dict(uuid=uuid)
        r = self.session.get(self.disco_url + 'get_tree', params=params)
        return ujson.loads(r.text)
        return r.json()

    def is_done(self, uuid):
        params = dict(uuid=uuid)
        r = self.session.get(self.disco_url + 'is_done', params=params)
        return ujson.loads(r.text)['done']
        return r.json()['done']

    def get_valid_actions(self, uuid):
        params = dict(uuid=uuid)
        r = self.session.get(self.disco_url + 'get_valid_actions', params=params)
        return ujson.loads(r.text)
        return r.json()

    def perform_action(self, uuid, action):
        params = dict(uuid=uuid)
        r = self.session.post(self.disco_url + 'perform_action', params=params, json=action)
        return ujson.loads(r.text)
        return r.json()

    def get_loss(self, uuid):
        params = dict(uuid=uuid)
        r = self.session.get(self.disco_url + 'get_loss', params=params)
        return ujson.loads(r.text)['loss']
        return r.json()['loss']

    def get_features(self, uuid):
        params = dict(uuid=uuid)
        r = self.session.get(self.disco_url + 'get_features', params=params)
        feats = ujson.loads(r.text)
        feats2 = dict()
        for k in feats:
            k = str(k)
            feats2[k] = []
            for pair in feats[k]:
                feats2[k].append((pair[0].encode('utf8'), pair[1]))
        return feats2

    def get_gold_action(self, uuid):
        params = dict(uuid=uuid)
        r = self.session.get(self.disco_url + 'get_gold_action', params=params)
        return ujson.loads(r.text)
        return r.json()

class ParsingSession(object):
    def __init__(self, session, id):
        self.session = session
        self.uuid = session.start_parse(id)

    def stop(self):
        self.session.stop_parse(self.uuid)

    def is_done(self):
        return self.session.is_done(self.uuid)

    def valid_actions(self):
        return self.session.get_valid_actions(self.uuid)

    def perform_action(self, action):
        return self.session.perform_action(self.uuid, action)

    def loss(self):
        return self.session.get_loss(self.uuid)

    def features(self):
        return self.session.get_features(self.uuid)

    def gold_action(self):
        return self.session.get_gold_action(self.uuid)

class DiscourseParser(pyvw.SearchTask):

    STRUCTURE = 1
    LABEL = 2
    NUCLEUS = 3

    SHIFT = dict(action='shift')
    REDUCE = dict(action='reduce', nucleus='left', label='comparison')

    def __init__(self, vw, sch, num_actions):
        pyvw.SearchTask.__init__(self, vw, sch, num_actions)
        sch.set_options(sch.AUTO_CONDITION_FEATURES)
        self.disco = DiscoSession()

    def _run(self, doc_id):
        parser = ParsingSession(self.disco, doc_id)
        output = []
        n = 1
        while not parser.is_done():
            actions = parser.valid_actions()
            action_labels = [self.get_label(a) for a in actions]
            features = parser.features()
            gold = parser.gold_action()
            ref = self.get_label(gold)
            with self.vw.example(features) as ex:
                pred = self.sch.predict(examples=ex, my_tag=n, oracle=ref, allowed=action_labels, condition=(n-1, 'p'))
                action = self.get_action(pred)
                # print 'id', doc_id
                # print 'session', parser.uuid
                # print 'labels', action_labels
                # print 'valid1', actions
                # print 'valid2', parser.valid_actions()
                # print 'gold', gold
                # print 'ref', ref
                # print 'pred', pred
                # print 'performing action', action
                # print
                parser.perform_action(action)
                output.append(pred)
                n += 1
        loss = parser.loss()
        self.sch.loss(loss)
        parser.stop()
        print 'parsed doc', doc_id, 'with loss', loss
        return output

    def get_label(self, action):
        return 1 if action['action'] == 'shift' else 2

    def get_action(self, label):
        return self.SHIFT if label == 1 else self.REDUCE

if __name__ == '__main__':
    disco = DiscoSession()
    dataset = disco.get_doc_ids()
    vw = pyvw.vw("--search 2 --quiet --search_task hook --ring_size 1024 --search_no_caching -f disco.vw")
    parser = vw.init_search_task(DiscourseParser)

    print 'training ...'
    for i in xrange(5):
        parser.learn(dataset)

    vw.finish()
    print 'done!'
