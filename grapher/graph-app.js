

const HIDE_TITLE = {
    "BoxScript": true,
    "BoxFunctionDeclaration": true
}

function setupModeControls() {
    const mode = localStorage.getItem("mode") || "WIDE";

    setMode(mode);

    document.querySelector("#layoutButtonPacked").addEventListener("click", e => setMode("PACKED"));
    document.querySelector("#layoutButtonWide").addEventListener("click", e => setMode("WIDE"));
}

let id = 0;
let lastAST = null;

let vizOptions = {
    nodes: {
        shadow: true,
        color: "#FFFFFF",
        shape: "box"
    },
    physics: {
        enabled: true,
        hierarchicalRepulsion: {
            avoidOverlap: 1
        }
    },
    layout: {

        hierarchical: {
            edgeMinimization: false,
            nodeSpacing: 25,
            parentCentralization: false
        },

    }
};

function getNextId() {
    return id++;
}

function setup() {
    const inputs = Array.from(document.querySelectorAll("a.fileName"))

    inputs.forEach(a => {
        a.addEventListener("click", async (e) => {
            e.preventDefault();
            loadSelection(a);
        });
    });

    if (inputs.length) {
        loadSelection(inputs[0])
    }

    setupModeControls();
}

function loadSelection(aTag) {
    fetchFile(aTag).then(redrawGraph);

    Array.from(document.querySelectorAll("a.file--selected")).forEach(a => a.classList.remove("file--selected"));

    aTag.classList.add("file--selected");
}



const fetchFile = async (aTag) => {
    const res = await fetch("/data/" + aTag.innerText);
    const data = await res.json();

    return data;
};

function redrawGraph(rawAST) {
    lastAST = rawAST;
    const transformedAST = convertRawASTNode(rawAST);
    const dataSetTree = astToVizDataSetTree(transformedAST);
    const nodes = extractNodes([], dataSetTree);
    const edges = extractEdges([], dataSetTree);

    // create a network
    var container = document.getElementById("display-panel");
    var data = {
        nodes: new vis.DataSet(nodes),
        edges: new vis.DataSet(edges),
    };

    var network = new vis.Network(container, data, vizOptions);
}

function extractNodes(array, transformedASTNode) {
    array.push(transformedASTNode);

    if (transformedASTNode.children) {
        transformedASTNode.children.map((child) => extractNodes(array, child));
    }

    return array;
}

function extractEdges(array, transformedASTNode) {
    if (!transformedASTNode.children) {
        return array;
    }

    for (let child of transformedASTNode.children) {
        array.push({ from: transformedASTNode.id, to: child.id });
        extractEdges(array, child);
    }

    return array;
}

function astToVizDataSetTree(transformedAST) {
    if (Array.isArray(transformedAST)) {
        return Array.prototype.concat.apply(transformedAST.map(astToVizDataSetTree));
    }

    const converted = {
        id: getNextId(),
        label: transformedAST.ASTType,
        title: transformedAST.sourceText || transformedAST.value
    };

    if (converted.title && !HIDE_TITLE[transformedAST.ASTType]) {
        converted.label += String.fromCharCode(10) + (converted.title.length <= 10 ? converted.title : converted.title.slice(0, 10) + "...");
    }

    converted.children = transformedAST.children ? transformedAST.children.map(astToVizDataSetTree) : [];

    return converted;
}

function vizDataSetToEdges(vizDataset) {
    return vizDataset.reduce((acc, node) => {
        if (!node.children) {
            return acc;
        }
        return acc.concat(node.children.map(c => { return { from: node.id, to: c.id } }));
    }, []);
}


function convertRawASTNode(rawNode) {
    if (Array.isArray(rawNode)) {
        return rawNode.map(convertRawASTNode);
    }
    else if (typeof rawNode === 'object') {
        const node = Object.keys(rawNode).reduce((acc, key) => {
            if (key.match(/ASTType|sourceText|position|safe/i)) {
                acc[key] = rawNode[key];
            }
            else if (typeof rawNode[key] === 'object') {
                let val = convertRawASTNode(rawNode[key]);
                val = Array.isArray(val) ? val : [val];

                if (val.length == 1) {
                    val = val[0];
                    val.ASTType = key + '/' + val.ASTType;

                    acc.children.push(val);
                }
                else {
                    acc.children.push({
                        ASTType: key,
                        children: val
                    });
                }

            }

            return acc;
        }, { children: [] });

        if (!node.sourceText && node.children.length == 1) {
            const newNode = Object.assign({}, node.children[0]);

            newNode.ASTType = node.ASTType + '/' + newNode.ASTType
            return newNode;
        }

        return node;
    }

    return rawNode;
}

function calculateBounds(root, dx, dy) {
    let y0 = Infinity;
    let y1 = -y0;
    let x0 = Infinity;
    let x1 = -y0;

    root.each(d => {
        if (d.y > y1) y1 = d.y;
        if (d.y < y0) y0 = d.y;
        if (d.x > x1) x1 = d.x;
        if (d.x < x0) x0 = d.x;
    });

    const height = y1 - y0 + dy * 2;
    const width = x1 + (0 - x0) + 200;

    return { x0, y0, x1, y1, width, height };
}

// adapted from https://observablehq.com/@d3/tree/2?intent=fork
function setupChart(data) {
    const root = d3.hierarchy(data);
    const nodeWidth = 10;
    const nodeHeight = 50;

    const tree = d3.tree().nodeSize([nodeWidth, nodeHeight]);

    tree.separation((a, b) => {
        let sep = b.data.ASTType.length / 2;

        sep += a.parent == b.parent ? 2 : 5;

        return sep
    });

    tree(root);

    const { x0, y0, width, height } = calculateBounds(root, nodeWidth, nodeHeight);

    const svg = d3.create("svg")
        .attr("overflow", "scroll")
        .attr("id", "astGraph")
        .attr("width", width)
        .attr("height", height)
        .attr("viewBox", [x0 - (nodeWidth * 4), y0 - nodeHeight, width, height])
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
            .x(d => d.x)
            .y(d => d.y));

    const node = svg.append("g")
        .attr("stroke-linejoin", "round")
        .attr("stroke-width", 3)
        .selectAll()
        .data(root.descendants())
        .join("g")
        .attr("transform", d => {
            return `translate(${d.x},${d.y})`;
        });

    node.append("title")
        .text(d => d.data.sourceText)

    node.append("circle")
        .attr("fill", d => d.children ? "#555" : "#999")
        .attr("r", 2.5);

    node.append("text")
        .attr("dy", "0.31em")
        .attr("x", d => 6)
        .attr("text-anchor", d => "start")
        .attr("class", d => d.data.sourceText ? 'node-hasSourceText' : '')
        .text(d => `${d.data.ASTType}`)
        .clone(true).lower()
        .attr("stroke", "white");

    node.append("text")
        .attr("y", "20")
        .attr("text-anchor", d => "middle")
        .attr("class", d => d.data.children && d.data.children.length || !(d.data.sourceText) ? 'node-hidden' : '')
        .text(d => `${d.data.sourceText}`)
        .attr("fill", "red")
        .clone(true).lower();


    return svg.node();
}

function setMode(mode) {
    if (mode === "WIDE") {
        vizOptions = {
            nodes: {
                shadow: true,
                color: "#FFFFFF",
                shape: "box"
            },
            physics: {
                enabled: false
            },
            layout: {
                hierarchical: {
                    nodeSpacing: 150
                },

            }
        };
        document.querySelector("#layoutButtonPacked").classList.remove("layout-control-button--selected");
        document.querySelector("#layoutButtonWide").classList.add("layout-control-button--selected");
        localStorage.setItem("mode", "WIDE");

        if (lastAST) {
            redrawGraph(lastAST);
        }
    }
    else if (mode === "PACKED") {
        vizOptions = {
            nodes: {
                shadow: true,
                color: "#FFFFFF",
                shape: "box"
            },
            physics: {
                enabled: true,
                hierarchicalRepulsion: {
                    avoidOverlap: 1
                }
            },
            layout: {

                hierarchical: {
                    nodeSpacing: 25
                }

            }
        };
        document.querySelector("#layoutButtonPacked").classList.add("layout-control-button--selected");
        document.querySelector("#layoutButtonWide").classList.remove("layout-control-button--selected");
        localStorage.setItem("mode", "PACKED");

        if (lastAST) {
            redrawGraph(lastAST);
        }
    }
}

setup();
