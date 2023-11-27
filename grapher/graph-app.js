const inputArea = document.querySelector( '#code-panel textarea' );

// inputArea.addEventListener( 'change', () => {
//     console.log( inputArea.value );
// });

const data = {
    name: "script",
    children: [
        {
            name: 'assignment',
            text: 'result = 1',
            children: [
                {
                    name: "identifier",
                    text: "result",
                    children: [
                        {
                            name: "literal",
                            text: "1"
                        },
                        {
                            name: "literal",
                            text: "1"
                        },
                        // {
                        //     name: "literal",
                        //     text: "1"
                        // }
                    ]
                },
                {
                    name: "literal",
                    text: "1"
                }
            ]
        }
    ]
};

const rawAST = {
    "sourceText": "result = 1;\nwhile( true ) {\n    result = result + 1;\n\tif( result > \"10\" ) {\n\t\tbreak;\n\t}\n}",
    "name": "BoxScript",
    "statements": [
      {
        "op": "Equal",
        "left": {
          "sourceText": "result",
          "name": "result",
          "safe": false,
          "position": {
            "start": {
              "line": 1,
              "column": 0
            },
            "end": {
              "line": 1,
              "column": 0
            }
          }
        },
        "sourceText": "result = 1",
        "name": "BoxAssignment",
        "position": {
          "start": {
            "line": 1,
            "column": 0
          },
          "end": {
            "line": 1,
            "column": 9
          }
        },
        "right": {
          "sourceText": "1",
          "name": "BoxIntegerLiteral",
          "position": {
            "start": {
              "line": 1,
              "column": 9
            },
            "end": {
              "line": 1,
              "column": 9
            }
          },
          "value": "1"
        }
      },
      {
        "condition": {
          "sourceText": "true",
          "name": "BoxBooleanLiteral",
          "position": {
            "start": {
              "line": 2,
              "column": 7
            },
            "end": {
              "line": 2,
              "column": 7
            }
          },
          "value": "true"
        },
        "sourceText": "while( true ) {\n    result = result + 1;\n\tif( result > \"10\" ) {\n\t\tbreak;\n\t}\n}",
        "name": "BoxWhile",
        "position": {
          "start": {
            "line": 2,
            "column": 0
          },
          "end": {
            "line": 7,
            "column": 0
          }
        },
        "body": [
          {
            "op": "Equal",
            "left": {
              "sourceText": "result",
              "name": "result",
              "safe": false,
              "position": {
                "start": {
                  "line": 3,
                  "column": 4
                },
                "end": {
                  "line": 3,
                  "column": 4
                }
              }
            },
            "sourceText": "result = result + 1",
            "name": "BoxAssignment",
            "position": {
              "start": {
                "line": 3,
                "column": 4
              },
              "end": {
                "line": 3,
                "column": 22
              }
            },
            "right": {
              "left": {
                "sourceText": "result",
                "name": "result",
                "safe": false,
                "position": {
                  "start": {
                    "line": 3,
                    "column": 13
                  },
                  "end": {
                    "line": 3,
                    "column": 13
                  }
                }
              },
              "sourceText": "result + 1",
              "name": "BoxBinaryOperation",
              "position": {
                "start": {
                  "line": 3,
                  "column": 13
                },
                "end": {
                  "line": 3,
                  "column": 22
                }
              },
              "right": {
                "sourceText": "1",
                "name": "BoxIntegerLiteral",
                "position": {
                  "start": {
                    "line": 3,
                    "column": 22
                  },
                  "end": {
                    "line": 3,
                    "column": 22
                  }
                },
                "value": "1"
              },
              "operator": "Plus"
            }
          },
          {
            "condition": {
              "left": {
                "sourceText": "result",
                "name": "result",
                "safe": false,
                "position": {
                  "start": {
                    "line": 4,
                    "column": 5
                  },
                  "end": {
                    "line": 4,
                    "column": 5
                  }
                }
              },
              "sourceText": "result > \"10\"",
              "name": "BoxComparisonOperation",
              "position": {
                "start": {
                  "line": 4,
                  "column": 5
                },
                "end": {
                  "line": 4,
                  "column": 17
                }
              },
              "right": {
                "sourceText": "\"10\"",
                "name": "BoxStringLiteral",
                "position": {
                  "start": {
                    "line": 4,
                    "column": 14
                  },
                  "end": {
                    "line": 4,
                    "column": 17
                  }
                },
                "value": "10"
              },
              "operator": "GreaterThan"
            },
            "sourceText": "if( result > \"10\" ) {\n\t\tbreak;\n\t}",
            "name": "BoxIfElse",
            "thenBody": [
              {
                "sourceText": "break;",
                "name": "BoxBreak",
                "position": {
                  "start": {
                    "line": 5,
                    "column": 2
                  },
                  "end": {
                    "line": 5,
                    "column": 7
                  }
                }
              }
            ],
            "elseBody": [],
            "position": {
              "start": {
                "line": 4,
                "column": 1
              },
              "end": {
                "line": 6,
                "column": 1
              }
            }
          }
        ]
      }
    ],
    "position": {
      "start": {
        "line": 1,
        "column": 0
      },
      "end": {
        "line": 7,
        "column": 0
      }
    }
  };

// const chartNode = setupChart( data );
const chartNode = setupChart( convertRawASTNode(rawAST) );

document.querySelector( '#display-panel' ).appendChild( chartNode );

function convertRawASTNode( rawNode ){
    if( Array.isArray( rawNode ) ){
        return rawNode.map( convertRawASTNode );
    }
    else if( typeof rawNode === 'object' ){
        return Object.keys( rawNode ).reduce( ( acc, key ) => {
            if( key.match( /name|sourceText|position|safe/i ) ){
                acc[ key ] = rawNode[ key ];
            }
            else if( typeof rawNode[ key ] === 'object' ){
                const val = convertRawASTNode( rawNode[ key ] );
                acc.children.push({
                    name: key,
                    children: Array.isArray( val ) ? val : [ val ]
                });
            }
    
            return acc;
        }, { children: [] } );
    }

    return rawNode;
}




function setupChart( data ){
    // debugger;
    // const width = 928;
      
    // Compute the tree height; this approach will allow the height of the
    // SVG to scale according to the breadth (width) of the tree layout.
    const root = d3.hierarchy(data);
    const dx = 10;
    // const dy = width / (root.height + 1);
    const dy = 50;
    
    // Create a tree layout.
    const tree = d3.tree().nodeSize([dx, dy]);
    
    tree.separation( (a,b) => {
        return a.parent == b.parent ? 8 : 16;
    });
    
    // Sort the tree and apply the layout.
    root.sort((a, b) => d3.ascending(a.data.name, b.data.name));
    tree(root);
    
    // Compute the extent of the tree. Note that x and y are swapped here
    // because in the tree layout, x is the breadth, but when displayed, the
    // tree extends right rather than down.
    let y0 = Infinity;
    let y1 = -y0;
    root.each(d => {
        if (d.y > y1) y1 = d.y;
        if (d.y < y0) y0 = d.y;
    });

    let x0 = Infinity;
    let x1 = -y0;
    root.each(d => {
        if (d.x > x1) x1 = d.x;
        if (d.x < x0) x0 = d.x;
    });
    
    // Compute the adjusted height of the tree.
    const height = y1 - y0 + dy * 2;
    const width = (x1 - x0 + dx * 2) + 200;
    
    const svg = d3.create("svg")
        .attr("width", width)
        .attr("height", height)
        .attr("viewBox", [ 0 - ( width / 2 ) , y0 - dy, width, height])
        .attr("style", "max-width: 100%; height: auto; font: 10px sans-serif; background: white;");
    
    const link = svg.append("g")
        .attr("fill", "none")
        .attr("stroke", "#555")
        .attr("stroke-opacity", 0.4)
        .attr("stroke-width", 1.5)
        .selectAll()
        .data(root.links())
        .join("path")
            .attr("d", d3.linkVertical()
                .x(d => d.x )
                .y(d => d.y ));
    
    const node = svg.append("g")
        .attr("stroke-linejoin", "round")
        .attr("stroke-width", 3)
        .selectAll()
        .data(root.descendants())
        .join("g")
        .attr("transform", d => {
            return `translate(${d.x},${d.y})`;
        });

    node
        .append( "title" )
        .text( d => d.data.sourceText )
    
    node.append("circle")
        .attr("fill", d => d.children ? "#555" : "#999")
        .attr("r", 2.5);
    
    node.append("text")
        .attr("dy", "0.31em")
        .attr("x", d =>  6)
        .attr("text-anchor", d => "start" )
        .attr( "class", d => d.data.sourceText ? 'node-hasSourceText' : '' )
        .text(d => `${d.data.name}`)
        .clone(true).lower()
        .attr("stroke", "white");

        node.append("text")
            .attr("dy", "0.31em")
            .attr("y", "15px")
            .attr("text-anchor", d => "middle" )
            .attr( "class", d=> d.data.children && d.data.children.length || !(d.data.sourceText) ? 'node-hidden' : '' )
            .text(d => `${d.data.sourceText}`)
            .attr( "fill", "red" )
            .clone(true).lower();
        
    
    return svg.node();
}