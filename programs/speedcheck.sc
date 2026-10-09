// speedcheck.sc - measure contraption processing speed (Carpet scarpet, MC 1.21.x)
//
// /speedcheck save <from> <to> <name>    count items in containers in area, store as <name>
// /speedcheck list | info <name> | delete <name>
// /speedcheck test <pos> <on|off> [name] sprint, time from <pos> in opposite state until it switches INTO the given state
//                                        without a set only the time is reported
// /speedcheck stop                       abort running test
// /speedcheck idealoutput <from> <to>    verify single-item boxes in area are ideally packed

__config() -> {
    'scope' -> 'global',
    'stay_loaded' -> true,
    'commands' -> {
        'save <from> <to> <name>' -> 'cmd_save',
        'list' -> 'cmd_list',
        'info <set>' -> 'cmd_info',
        'delete <set>' -> 'cmd_delete',
        'test <pos> <state>' -> _(pos, state) -> cmd_test(pos, state, null),
        'test <pos> <state> <set>' -> 'cmd_test',
        'stop' -> 'cmd_stop',
        'idealoutput <from> <to>' -> 'cmd_idealoutput',
    },
    'arguments' -> {
        'name' -> {'type' -> 'term', 'suggest' -> ['Test_Set_1']},
        'set' -> {'type' -> 'term', 'suggester' -> _(args) -> keys(global_sets)},
        'from' -> {'type' -> 'pos', 'loaded' -> true},
        'to' -> {'type' -> 'pos', 'loaded' -> true},
        'pos' -> {'type' -> 'pos', 'loaded' -> true},
        'state' -> {'type' -> 'term', 'options' -> ['on', 'off']},
    },
};

global_sets = read_file('sets', 'json') || {};
global_test = null;

_save_sets() -> write_file('sets', 'json', global_sets);

_add(items, id, count) -> (
    id = replace(id, 'minecraft:', '');
    items:id = (items:id || 0) + count;
);

// Reads each block entity's own 'Items' NBT (not inventory_get: carpet merges
// double chests into one 54-slot inventory, which would count both halves twice).
_count_area(from, to) -> (
    items = {};
    volume(from, to,
        data = block_data(_);
        if (data,
            for (parse_nbt(data):'Items' || [],
                // shulker box: count contents (if any), never the box itself
                if (_:'id' ~ 'shulker_box$',
                    for (_:'components':'minecraft:container' || [], _add(items, _:'item':'id', _:'item':'count' || 1)),
                    _add(items, _:'id', _:'count' || 1)
                )
            )
        )
    );
    s64 = 0; s16 = 0; s1 = 0;
    for (keys(items),
        lim = stack_limit(_);
        if (lim == 64, s64 += items:_, lim == 16, s16 += items:_, s1 += items:_)
    );
    {'items' -> items, 's64' -> s64, 's16' -> s16, 's1' -> s1, 'total' -> s64 + s16 + s1}
);

_print_set(name, set) -> (
    print(str('%s: %d items, %d types (64x: %d | 16x: %d | 1x: %d)', name, set:'total', length(set:'items'), set:'s64', set:'s16', set:'s1'));
);

_err(msg) -> (print(format('r ' + msg)); null);

// tick events have no command source: send to all players, or console if none online
_say(msg) -> if (p = player('all'), print(p, msg), print(msg));

// scarpet run() uses carpet rule commandScriptACE as permission level (default ops = 2); /tick needs 3
_tick(cmd) -> (
    err = run('tick ' + cmd):2;
    if (err, _say(format('r /tick ' + cmd + ' failed: ' + err + '. Fix: /carpet setDefault commandScriptACE 3')));
    !err
);

cmd_save(from, to, name) -> (
    set = _count_area(from, to);
    global_sets:name = set;
    _save_sets();
    _print_set(name, set);
    null
);

cmd_list() -> (
    if (!global_sets, return(_err('No saved sets')));
    print('Sets: ' + join(', ', map(sort(keys(global_sets)), str('%s (%d)', _, global_sets:_:'total'))));
    null
);

cmd_info(name) -> (
    if (!has(global_sets, name), return(_err('Unknown set: ' + name)));
    set = global_sets:name;
    items = set:'items';
    _print_set(name, set);
    print(format('g   ' + join(', ', map(sort_key(keys(items), -items:_), str('%s %d', _, items:_)))));
    null
);

cmd_delete(name) -> (
    if (!has(global_sets, name), return(_err('Unknown set: ' + name)));
    delete(global_sets, name);
    _save_sets();
    print('Deleted ' + name);
    null
);

cmd_test(pos, state, name) -> (
    if (global_test, return(_err('A test is already running, use /speedcheck stop')));
    if (name && !has(global_sets, name), return(_err('Unknown set: ' + name)));
    if (name && global_sets:name:'total' == 0, return(_err('Set ' + name + ' has 0 items')));
    if (!_tick('unfreeze'), return());
    target = state == 'on';
    counting = (power(pos) > 0) != target;
    global_test = {
        'pos' -> pos,
        'dim' -> current_dimension(),
        'set' -> name,
        'target' -> target,
        'counting' -> counting,
        'ticks' -> 0,
        'start_ms' -> unix_time(),
    };
    _tick('sprint 3650d');
    _say(str('Test started%s, %s %s turns %s', if (name, str(': %s (%d items)', name, global_sets:name:'total'), ''),
        if (counting, 'timing until', 'timer starts when'), pos, if (counting, state, if (target, 'off', 'on'))));
    null
);

cmd_stop() -> (
    if (!global_test, return(_err('No test running')));
    _finish(false);
    null
);

// Timer starts when <pos> is in the opposite of the target state, and stops when it switches into the target state.
__on_tick() -> (
    if (!global_test, return());
    t = global_test;
    cur = in_dimension(t:'dim', power(t:'pos') > 0);
    if (!t:'counting',
        if (cur != t:'target',
            global_test:'counting' = true;
            global_test:'start_ms' = unix_time();
            _say(str('Timer started, ends when %s turns %s', t:'pos', if (t:'target', 'on', 'off')))
        ),
        global_test:'ticks' = t:'ticks' + 1;
        if (cur == t:'target', _finish(true))
    );
);

_finish(completed) -> (
    t = global_test;
    global_test = null;
    _tick('sprint stop');
    _tick('freeze');
    ticks = t:'ticks';
    real_s = (unix_time() - t:'start_ms') / 1000;
    _say(if (t:'set', t:'set' + ' ', 'Test ') + if (completed, 'finished', 'aborted'));
    if (completed && t:'set',
        total = global_sets:(t:'set'):'total';
        rate = total * 72000 / max(ticks, 1);
        _say(str('  Speed: %.0f items/h (%.2fx hopper)', rate, rate / 9000));
        _say(str('  Items: %d', total))
    );
    _say(str('  Game time: %d gt | %.2f s | %.2f min | %.4f h', ticks, ticks / 20, ticks / 1200, ticks / 72000));
    _say(str('  Real time: %.2f s', real_s));
);

// Item key: id, plus custom name and other components, since filters treat those as different items.
_key(item) -> (
    c = copy(item:'components') || {};
    key = replace(item:'id', 'minecraft:', '');
    name = c:'minecraft:custom_name';
    if (name, delete(c, 'minecraft:custom_name'); key += ' \'' + replace(name, '^"|"$', '') + '\'');
    if (c, key += ' ' + encode_json(c));
    key
);

// Ideal output: per item, boxes = ceil(total / box capacity) and at most one partial box.
cmd_idealoutput(from, to) -> (
    boxes = {}; loose = {}; mixed = 0; empty = 0;
    volume(from, to,
        data = block_data(_);
        if (data,
            for (parse_nbt(data):'Items' || [],
                if (_:'id' ~ 'shulker_box$',
                    content = {};
                    for (_:'components':'minecraft:container' || [], k = _key(_:'item'); content:k = (content:k || 0) + (_:'item':'count' || 1));
                    ids = keys(content);
                    if (!ids, empty += 1,
                        length(ids) > 1, mixed += 1,
                        id = ids:0; if (!has(boxes, id), boxes:id = []); boxes:id += content:id
                    ),
                    _add(loose, _:'id', _:'count' || 1)
                )
            )
        )
    );
    if (!boxes, return(_err('No single-item shulker boxes found')));
    // ideal per item type: ceil(total / cap) boxes, at most one partial. percentage = ideal types / all types
    n = 0; n_part = 0; n_ideal = 0; n_ideal_part = 0; bad = [];
    for (sort(keys(boxes)),
        id = _;
        counts = boxes:id;
        cap = 27 * stack_limit(split(' ', id):0);
        total = reduce(counts, _a + _, 0);
        ideal_part = if (total % cap, 1, 0);
        ideal = floor(total / cap) + ideal_part;
        part = length(filter(counts, _ < cap));
        n += length(counts); n_part += part; n_ideal += ideal; n_ideal_part += ideal_part;
        if (length(counts) != ideal || part > ideal_part,
            put(bad, null, str('    %s: %d boxes (%d partial), ideal %d (%d partial)', id, length(counts), part, ideal, ideal_part))
        )
    );
    n += mixed;
    types = length(boxes);
    print(str('Ideal output: %s - %d/%d types ideal (%.1f%%)', if (!bad && !mixed, 'YES', 'NO'), types - length(bad), types, 100 * (types - length(bad)) / types));
    print(str('  Boxes: %d (%d partial), ideal %d (%d partial), %d extra', n, n_part, n_ideal, n_ideal_part, n - n_ideal));
    if (bad, print('  Not ideal:'); for (bad, print(_)));
    if (mixed, print('  Mixed boxes: ' + mixed));
    if (empty, print(format('g   Empty boxes (ignored): ' + empty)));
    if (loose, print(format('g   Loose items (ignored): ' + join(', ', map(keys(loose), str('%s %d', _, loose:_))))));
    null
);
