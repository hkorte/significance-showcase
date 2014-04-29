app.controller("MainController", function($scope, $http){
	$scope.selectedIndex = "wikipedia";
	$scope.selectedType = "page";
	$scope.selectedField = "text";
	$scope.selectedSize = 15;
	$scope.query = "{\n\t\"query_string\": {\n\t\t\"default_field\": \"text\",\n\t\t\"query\": \"geek\"\n\t}\n}";
	$scope.results = {};
	$scope.termStats = {};
	$scope.table = [];
	$scope.selectedTerm = null;
	$scope.search = function() {
		var request = {
			field: $scope.selectedField,
			size: $scope.selectedSize,
			query: $scope.query
		}
		$http.post("/"+$scope.selectedIndex+"/"+$scope.selectedType+"/_significance", request).success(function(data) {
			var termStats = {};
			var table = [];
			for(var i = 0; i < request.size; i++) {
				table.push({});
			}
			for(measure in data) {
				var array = data[measure];
				for(var i = 0; i < array.length; i++) {
					table[i][measure] = array[i];
					var termStatsObj = termStats[array[i].term] || {};
					jQuery.extend(termStatsObj, array[i]);
					termStats[termStatsObj.term] = termStatsObj;
				}
			}
			for(term in termStats) {
				var thisTerm = termStats[term];
				thisTerm.n0x = thisTerm.n00 + thisTerm.n01;
				thisTerm.nx0 = thisTerm.n00 + thisTerm.n10;
				thisTerm.n1x = thisTerm.n10 + thisTerm.n11;
				thisTerm.nx1 = thisTerm.n01 + thisTerm.n11;
				thisTerm.nxx = thisTerm.n0x + thisTerm.n1x;
				thisTerm.subsetProbability = thisTerm.n11 / thisTerm.nx1; // compute this again to have it for each measure
				thisTerm.supersetProbability = thisTerm.n1x / thisTerm.nxx; // compute this again to have it for each measure
			}
			$scope.table = table;
			$scope.termStats = termStats;
			$scope.results = data;
		});
	};
	$scope.selectTerm = function(term) {
		$scope.selectedTerm = $scope.termStats[term];

	}
});