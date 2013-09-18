var app = angular.module('CreateProcessApp', []);

app.controller('ProcessController', ['$scope', '$http', function($scope, $http){
	
	$scope.process = [];
	$scope.activities = [];
	$scope.node = [];
	$scope.count = [{"index":0}];
	
	
	$http.get('/nodes/all')
	.success(function(responseData){
		$scope.nodes = responseData;
	}).error(function(errorData){
		$scope.error = errorData;
	});
	
	
	$scope.nextAction = function() {
		$scope.count.push({"index":$scope.count.length});
	}


	$scope.updateActivities = function(index) {
		$http.get('/nodes/reflection/' + $scope.node[index].nodeId)
		.success(function(responseData){
			if(index==0) {
				$scope.activities[index] = responseData.triggers;
				$scope.process[index] = {
						"node":$scope.node[index].nodeId, 
						"name":$scope.node[index].nodeName, 
						"logo":$scope.node[index].nodeLogo, 
						"description":$scope.node[index].nodeDescription, 
						"data":responseData.triggers[0]
				};
			} else {
				$scope.activities[index] = responseData.actions;
				$scope.process[index] = {
						"node":$scope.node[index].nodeId, 
						"name":$scope.node[index].nodeName, 
						"logo":$scope.node[index].nodeLogo, 
						"description":$scope.node[index].nodeDescription, 
						"data":responseData.actions[0]
				};
			}
		}).error(function(errorData){
			$scope.error = errorData;
		});
	}
		
	
	$scope.getData = function(data, node, source) {
		data.showWorking = true;
		$http.get('/nodes/reflection/' + node + '/info/' + source)
		.success(function(responseData){
			data.data = responseData;
			data.showData = true;
			data.showWorking = false;
		}).error(function(errorData){
			$scope.error = errorData;
		});
	}
	
	
	$scope.showOutput = function() {
		console.log("****OUTPUT******");
		console.log(JSON.stringify($scope.process));
	}
	

	$scope.addText = function(model, nodeIndex, value) {
		if(model.value)
			model.value = model.value + ' ##'+nodeIndex+'.'+value+'##';
		else
			model.value = '##'+nodeIndex+'.'+value+'##';
			
	}

}]);

/**
app.directive('nodeDropdown', function() {
	return {
		restrict: 'E',
		template: "<select ng-model='selected' ng-options='d.name for d in data' class='form-control'></select>",
		scope: {
			selected: '=',
			source: '@',
			node: '@'
		},
		controller: ['$scope', '$http', function($scope, $http) {
			$scope.getData = function(selected, source, node) {
				console.log("Selected : " + selected + " Source : " + source + " Node : " + node);
				$http.get('/nodes/reflection/' + node + '/info/' + source)
				.success(function(responseData){
					$scope.data = responseData;
					$scope.selected = responseData[0];
				}).error(function(errorData){
					$scope.error = errorData;
				});
			};
		}],
		link: function (scope, element, attrs, controller) {
			scope.getData(scope.selected, attrs.source, attrs.node);
		}
	}
});

**/