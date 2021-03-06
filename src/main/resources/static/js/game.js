/**
 * Class representing ongoing game, responsible of processing communication and sync with displaying
 *
 * @author Marek Bauer
 */
class Game {
    /**
     * Construction game object
     *
     * @param connection:Connection - connection to server
     * @param canvas - handler of canvas object
     * @param playerName:String - name of player
     * @param whenFinished - function to call when game is over
     */
    constructor(connection, canvas, playerName, whenFinished) {
        this.whenFinished = whenFinished;
        this.players = [];
        this.canvas = canvas;
        this.playersMap = new Map();
        this.board = Array(BOARD_SIZE);
        for (let i = 0; i < BOARD_SIZE; i++) {
            this.board[i] = Array(BOARD_SIZE);
            this.board[i].fill(BASE_PATTERN);
        }
        this.view = new BoardView(canvas, this.board, this.players);
        this.turn = -1;
        this.alive = true;
        this.step = NUMBER_OF_STEPS - 4;
        this.nextMove = 'EAST';
        this.playerName = playerName;
        this.connection = connection;
        this.connection.subscribe(this.update.bind(this))
            .then(init => this.initialize(init))
            .catch(error => {
                popup("Error", error, [["OK", () => {
                }]]);
                this.whenFinished();
            });
        this.gameLoop = null;
        $(document).keydown((function (e) {
            if (e.which === 37 || e.which === 65 || e.which === 97) {
                this.setNextMove('WEST');
            } else if (e.which === 38 || e.which === 87 || e.which === 119) {
                this.setNextMove('NORTH');
            } else if (e.which === 39 || e.which === 68 || e.which === 100) {
                this.setNextMove('EAST');
            } else if (e.which === 40 || e.which === 83 || e.which === 115) {
                this.setNextMove('SOUTH');
            }
        }).bind(this));
        $(document).on("swipe", (function (e) {
            let directions = e.detail.directions;
            if (directions.left) {
                this.setNextMove('WEST');
            } else if (directions.right) {
                this.setNextMove('EAST');
            } else if (directions.top) {
                this.setNextMove('NORTH');
            } else if (directions.bottom) {
                this.setNextMove('SOUTH');
            }
        }).bind(this));
        this.playerX = 100;
        this.playerY = 100;
        this.animator = new Animator((() => this.view.draw(this.playerX, this.playerY)).bind(this), FRAMES_PER_SECONDS);
        this.animator.start();
    }

    initialize(data) {
        console.log(data)
        this.gameStart = data.time;
        for (let p of data.addedPlayers) {
            let player = Player.fromServer(p);
            player.path = p.path.map(x => x.map(y => y * BLOCK_SIZE));
            this.players.push(player);
            this.playersMap.set(p.name, player);
            for (let [x, y] of p.fields) {
                this.board[x][y] = player.pattern;
            }
        }
    }

    /**
     * Return index of player in list of specific name
     *
     * @param players - list of player
     * @param name - name to be found
     * @returns {number} - index of player with specific name
     */
    static findPlayerIndexByName(players, name) {
        for (let i = 0; i < players.length; i++) {
            if (name === players[i].name) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Converting x, y coordinates to direction
     *
     * @param x
     * @param y
     * @returns {string} - direction string
     */
    static posToMove(x, y) {
        if (y > 0) return 'SOUTH';
        else if (y < 0) return 'NORTH';
        else if (x < 0) return 'WEST';
        else return 'EAST';
    }

    /**
     * Converts direction string to coordinates
     *
     * @param dir - direction
     * @returns {number[]} - [x coordinate, y coordinate]
     */
    static directionToMove(dir) {
        if (dir === 'NORTH') return [0, -1];
        else if (dir === 'SOUTH') return [0, 1];
        else if (dir === 'EAST') return [1, 0];
        else if (dir === 'WEST') return [-1, 0];
    }

    /**
     * Set next move of player
     *
     * @param nextMove - sting representing direction 'NORTH', 'SOUTH' etc.
     */
    setNextMove(nextMove) {
        this.nextMove = nextMove;
    }

    /**
     * Handling data from server
     *
     * @param data - data from server
     */
    update(data) {
        console.log(data);
        console.log("Behind: " + Number(this.turn - Number(data.turn)));
        if (this.turn === -1) {
            this.turn = data.turn;
            this.gameStart = (Date.now() - (1000 / TURNS_PER_SECONDS) * data.turn) - currentAvgDelay();
            this.step = data.turn * NUMBER_OF_STEPS;
            this.addPlayerList(data.addedPlayers);
            this.killPlayerList(data.killedPlayers);
            this.moveListLate(data.moves, 1);
            this.changeBoardList(data.changes, 0);
            this.gameLoop = setInterval((() => {
                this.play();
            }).bind(this), 1000 / (TURNS_PER_SECONDS * BLOCK_SIZE / SPEED))
        } else if (this.turn <= Number(data.turn)) {
            this.addPlayerList(data.addedPlayers);
            this.killPlayerList(data.killedPlayers);
            this.moveListInTime(data.moves);
            this.changeBoardList(data.changes, 0)
        } else {
            const behind = this.turn - Number(data.turn);
            this.addPlayerList(data.addedPlayers);
            this.killPlayerList(data.killedPlayers);
            this.moveListLate(data.moves, behind);
            this.changeBoardList(data.changes, behind)
        }

    }

    /**
     * Add new players to game
     *
     * @param newPlayers - object from server
     */
    addPlayerList(newPlayers) {
        for (let p of newPlayers) {
            const player = Player.fromServer(p);
            this.players.push(player);
            this.playersMap.set(p.name, player);
        }
    }

    /**
     * Remove players from game
     *
     * @param killedPlayers - object from server
     */
    killPlayerList(killedPlayers) {
        for (let p of killedPlayers) {
            if (p === this.playerName) {
                this.alive = false;
                this.whenFinished();
            }
            this.playersMap.delete(p);
            const index = Game.findPlayerIndexByName(this.players, p);
            let toDel = this.players[index];
            const patternToDel = toDel.pattern;
            for (let x = 0; x < BOARD_SIZE; x++) {
                for (let y = 0; y < BOARD_SIZE; y++) {
                    if (this.board[x][y] === patternToDel) {
                        this.view.changeField(x, y, this.board[x][y], BASE_PATTERN);
                        this.board[x][y] = BASE_PATTERN;
                    }
                }
            }
            this.players.splice(index, 1);
            this.view.killPlayer(toDel);
        }
    }

    /**
     * Make moves when data came from server in time
     *
     * @param moves - object from server
     */
    moveListInTime(moves) {
        for (let m of moves) {
            let p = this.playersMap.get(m.player);
            if (p !== undefined) {
                p.setFuturePos(m.x, m.y);
                if (m.havePath) {
                    p.drawPath();
                } else {
                    p.closePath();
                }
            }
        }
    }

    /**
     * Change field ownership
     *
     * @param changes - object from server
     * @param behind - number of turns behind current
     */
    changeBoardList(changes, behind) {
        if (behind <= 1) {
            for (const [player, list] of Object.entries(changes)) {
                for (let [x, y] of list) {
                    let patTo;
                    if (player === "") {
                        patTo = BASE_PATTERN;
                    } else {
                        patTo = this.playersMap.get(player).pattern;
                    }
                    if (patTo !== this.board[x][y]) {
                        this.view.changeField(x, y, this.board[x][y], patTo);
                        this.board[x][y] = patTo;
                    }
                }
            }
        } else {
            for (const [player, list] of Object.entries(changes)) {
                for (let [x, y] of list) {
                    let patTo;
                    if (player === "") {
                        patTo = BASE_PATTERN;
                    } else {
                        patTo = this.playersMap.get(player).pattern;
                    }
                    this.board[x][y] = patTo;
                }
            }
        }
    }

    /**
     * Correct moves if data came too late
     *
     * @param moves - object from server
     * @param turnsBehind - number of turns behind current
     */
    moveListLate(moves, turnsBehind) {
        if (turnsBehind === 1) {
            for (let m of moves) {
                let p = this.playersMap.get(m.player);
                if (p !== undefined) {
                    p.makePositionAdjustments(m.x, m.y, this.step);
                    if (m.havePath) {
                        p.drawPath();
                    } else {
                        p.closePath();
                    }
                }
            }
        } else if (turnsBehind === 2) {
            for (let m of moves) {
                let p = this.playersMap.get(m.player);
                if (p !== undefined) {
                    p.setPrevPos(m.x, m.y, this.step);
                    if (m.havePath) {
                        p.drawPath();
                    } else {
                        p.closePath();
                    }
                }
            }
        }
    }

    /**
     * Send move to server and change direction of player
     */
    sendMove() {
        let main = this.playersMap.get(this.playerName);
        let d = Game.directionToMove(this.nextMove);
        let curr = main.currentPos;
        let mX, mY;
        if (d[0] * main.movX >= 0 && d[1] * main.movY >= 0) {
            mX = d[0];
            mY = d[1];
        } else {
            this.nextMove = Game.posToMove(main.movX, main.movY);
            d = Game.directionToMove(this.nextMove);
            mX = d[0];
            mY = d[1];
        }
        main.setFuturePos(mX + curr.x, mY + curr.y);
        //this.turn = Math.floor((Date.now() - this.gameStart)/250) + 1;
        console.log(this.nextMove);
        this.connection.sendMove(this.turn, this.nextMove);
    }

    /**
     * Main game loop
     */
    play() {
        let expectedStep = Math.floor((Date.now() - this.gameStart) / (1000 / TURNS_PER_SECONDS / NUMBER_OF_STEPS));
        while (this.step <= expectedStep) {
            if (this.alive) {
                if (this.step % NUMBER_OF_STEPS === 0) {
                    this.turn += 1;
                } else if (this.step % NUMBER_OF_STEPS === MOVE_WINDOW) {
                    this.sendMove();
                }
            }
            for (let p of this.players) {
                p.move();
            }
            this.step = (this.step + 1);
            if (this.alive) {
                //const main = this.playersMap.get("Bot Marek");
                const main = this.playersMap.get(this.playerName); //TODO
                this.playerX = main.posX - this.canvas.width / 2 + PLAYER_RADIUS;
                this.playerY = main.posY - this.canvas.height / 2 + PLAYER_RADIUS;
            }
        }
    }

    /**
     * Stops whole game
     */
    kill() {
        this.animator.kill();
        this.connection.unsubscribe();
        clearInterval(this.gameLoop);
        $(document).off('keydown');
        $(document).off('swipe');
    }
}