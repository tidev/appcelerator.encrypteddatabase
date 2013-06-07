var moment = require('alloy/moment');

var dbType = require('appcelerator.encrypteddatabase').Database != null ? "enc.db" : "sql";

exports.definition = {
	config: {
		"columns": {
			"item":"text",
			"done":"integer",
			"date_completed":"text"
		},
		"adapter": {
			"type": dbType,
			"collection_name": "todo",
			"db_name": "todo." + dbType
		}
	},

	extendModel : function(Model) {
        _.extend(Model.prototype, {
            validate : function(attrs) {
                for (var key in attrs) {
                    var value = attrs[key];
                    if (value) {
                        if (key === "item") {
                            if (value.length <= 0) {
                                return 'Error: No item!';
                            }
                        }
                        if (key === "done") {
                            if (value.length <= 0) {
                                return 'Error: No completed flag!';
                            }
                        }
                    }
                }
            }
        });

        return Model;
    },

    extendCollection : function(Collection) {
        _.extend(Collection.prototype, {
        	comparator: function(todo) {
				return todo.get('done');
			}
        });

        return Collection;
    }
}

